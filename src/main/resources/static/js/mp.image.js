import {onEndSwipe, onStartSwipe, toggleFavorite} from "./mp.js";
import {fetchImageAt, fetchImageAtPosition, loadImage} from "./mp.image_repo.js";


console.log('mp.image.js was picked up')

const getSelected = (images) => {
    for (let i = 0; i < images.length; i++) {
        if (images[i].classList.contains('current')) {
            return i
        }
    }
    return 0
}

const setSelected = (url) => {
    let images = $('#image-viewer .Display-image')
    images.each((idx, pic) => {
        let picture = $(pic)
        let src = picture.find('img')[0].src
        src = src.substring(0, src.lastIndexOf('/'))
        let selected = url.substring(0, url.lastIndexOf('/'))
        src === selected ? picture.addClass('current') :
            picture.removeClass('current')
    })
}

const clearSelected = (images) =>
    Object.values(images).forEach(img => img?.classList && img.classList.remove('current'))

const setFavoriteButtonState = (current) => {
    const favButton = $('#Toggle-image-favorite-button')
    let selected
    !isNaN(current) ?
        selected = fetchImageAtPosition(current) :
        selected = fetchImageAt(current)
    console.log('fav? ', current, selected)
    if (selected === null || selected === undefined) {
        // comic book or video
        return
    }
    selected['favorite'] ?
        favButton.addClass('favorite') :
        favButton.removeClass('favorite')
}

const updateViewerState = (images, current) => {
    clearSelected(images)
    images[current].classList.add('current')
    setFavoriteButtonState(current)
    updatePageCounter(current, images.length)
}

const showPrevImage = (images) => {
    let current = getSelected(images)
    current = current <= 0 ? images.length - 1 : current - 1
    updateViewerState(images, current);
}

const showNextImage = (images) => {
    let current = getSelected(images)
    current = current >= images.length - 1 ? 0 : current + 1
    updateViewerState(images, current)
}

const isComic = (image) => {
    if (image === null) return false
    let {links} = image
    if (!links) return false
    let pageLink = links.find(link => link.rel.includes('page_'))
    return pageLink !== undefined && pageLink !== null
        && pageLink.href !== undefined && pageLink.href !== null
}

const createComicPage = (imgLink, favLink) => {
    let id = imgLink.href.match(/([\w-]{36})/g)[0]
    let div = $('<div>')
    div.addClass('Display-image')
    div.attr('data-fav-link', favLink)
    div.attr('id', id)

    let img = $('<img>')
    img.attr('src', imgLink.href)
    img.attr('alt', 'Comic page')

    div.append(img)
    return div
}

const getComicPages = (comic) => {
    const _id = 'COMIC_PAGE_CONTAINER'

    let favLink = comic.links.find(link => link.rel === 'favorite')?.href
    let pages = comic.links
        .filter(link => link.rel.includes('page_'))
        .map(link => createComicPage(link, favLink))

    let container = $('<div>', {id: _id, style: 'display: none'}).appendTo('body')
    container.append(pages)
    let images =  $(`#${_id}`).find('.Display-image')
    container.remove()
    return images
}

const onShowImageViewer = (images, selected, isComic) => {
    // prevent body scroll
    $('body').css('overflow', 'hidden')

    // create copies of image elements for viewer
    const viewer = $('#image-viewer')
    for (const img of images) {
        const clone = $(img).clone()
        viewer.append(clone)
    }

    const container = $('#image-viewer-container')
    container.css('display', 'block')
    attachImageCycleSwipe(container)

    const pageCounter = $('#Page-counter')
    if (isComic) {    // comic display
        pageCounter.css('visibility', 'visible')
        // show first page
        container.find('.Display-image').first().addClass('current')
        updateViewerState(images, 0)
    } else { // picture display
        setSelected(selected)
        pageCounter.css('visibility', 'hidden')
        let selectedIdx = getSelectedIndex(images, selected)
        console.log('selected IDX', selectedIdx)
        updateViewerState(images, selectedIdx)
    }
}

const onHideImageViewer = () => {
    $('body').css('overflow', 'revert')
    let container = document.getElementById('image-viewer-container')
    container.style.display = 'none'
    document.getElementById('image-viewer').innerHTML = ''
}

const onFavoriteImage = async () => {
    const favButton = $('#Toggle-image-favorite-button')
    const displayImages = $('#image-viewer-container').find('.Display-image')

    let selectedIdx = getSelected(displayImages)
    let selectedImage = $(displayImages[selectedIdx])
    let favLink = selectedImage.data('fav-link')

    await toggleFavorite(favLink, (response) => {
        loadImage(response)
        response['favorite'] ?
            favButton.addClass('favorite') :
            favButton.removeClass('favorite')
    })
}

export const cycleImages = (images) => setInterval(() => showNextImage(images), 1000)

export const updatePageCounter = (current, total) => {
    let oneIndex = current + 1
    let text = `${oneIndex} / ${total}`
    $('#Page-counter').text(text)
}

const getSelectedIndex = (cards, url) => {
    let selected = 0
    cards.each((idx, card) => {
        if ($(card).data('fav-link') === url) {
            selected = idx
        }
    })
    return selected
}

// TODO: implement ComicBookRepo, standardize data fetching from repos
export const attachImageHandlers = (image) => {
    const siblingSelector = 'div > .Picture-card > .Display-image'
    const imgCard = $(`#${image.id}`)
    if (isComic(image)) {
        let pages = getComicPages(image)
        imgCard.on('click', () => onShowImageViewer(pages, 0, true))
    } else {
        const siblingCards = imgCard.closest('.Picture-card-container').find(siblingSelector)
        let selected = image.links.find(link => link.rel === 'favorite').href
        imgCard.on('click', () => onShowImageViewer(siblingCards, selected, false))
    }
}

export const attachImageViewerHandlers = () => {
    const  container = $('#image-viewer-container')
    $('#Image-viewer-close-button').on('click', () => onHideImageViewer())
    $('#Toggle-image-favorite-button').on('click tap', () => onFavoriteImage())
    $(document).on('keydown', (e) => {
        let display = container.css('display')
        if (display && display !== 'none') {
            let images = container.find('.Display-image')
            if (e.key === 'ArrowLeft') {
                showPrevImage(images)
            }
            if (e.key === 'ArrowRight') {
                showNextImage(images)
            }
        }
    })
}

export const attachImageCycleSwipe = (element) => {
    let images = element.find('.Display-image')
    element.on('touchstart', (e) => onStartSwipe(e))
    element.on('touchend', (e) => onEndSwipe(e,
        () => showNextImage(images),
        () => showPrevImage(images)))
}
