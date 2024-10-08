import {getViewportDimensions, onEndSwipe, onStartSwipe, toggleFavorite} from "./mp.js";
import {fetchImageAt, fetchImageAtPosition, fetchImageById, fetchPictureCount, loadImage} from "./mp.image_repo.js";
import {fetchComic, fetchComicForPage, getComicPages, isComic, loadComic} from "./mp.comic_repo.js";
import {getState, setState} from "./mp.state.js";


console.log('mp.image.js was picked up')


const getDataUrl = (entity) => entity.links.find(link => link.rel === 'data')?.href

const getFavoriteUrl = (entity) => entity.links.find(link => link.rel === 'favorite')?.href

const getLinksArray = (_links) => {
    return Object.entries(_links).map((link) => ({
        rel: link[0],
        href: link[1].href
    }))
}

const setFavoriteButtonState = (isFav) => {
    const favButton = $('#Toggle-image-favorite-button')
    isFav ? favButton.addClass('favorite') :
        favButton.removeClass('favorite')
}

const updatePageCounter = (pages) => {
    const pageCounter = $('#Page-counter')
    if (pages) {
        let {current, total} = pages
        let displayPage = current + 1   // 1-indexed
        pageCounter.css('visibility', 'visible')
        pageCounter.text(`${displayPage} / ${total}`)
    } else {
        pageCounter.css('visibility', 'hidden')
    }
}

const createComicState = (url, prev, pages) => {
    let comic = fetchComicForPage(url)
    let comicPages = getComicPages(comic)
    let page = comicPages[prev].href
    return {
        url: page,
        isFav: comic.favorite,
        favLink: getFavoriteUrl(comic),
        pages: {
            current: prev,
            total: pages.total
        }
    }
}

const createPictureState = (prevPicture) => {
    return {
        url: getDataUrl(prevPicture),
        isFav: prevPicture.favorite,
        favUrl: getFavoriteUrl(prevPicture),
        pages: null,
    }
}

$(window).resize(() => {
    let display = $('#image-viewer-container').css('display')
    if (display === 'block') updateViewerState()
})

const adjustViewerOrientation = (url) => {
    let img = new Image()
    img.onload = () => {
        const viewer = $('#image-viewer')
        const viewerDisplay = $('#image-viewer-display')
        let {width: vw, height: vh} = getViewportDimensions()

        if (vw > vh && img.height > img.width) {    // landscape mode
            viewerDisplay.removeClass('CW')
            viewerDisplay.addClass('rotate CCW')
        } else if (vh > vw && img.width > img.height) {    // portrait mode
            viewerDisplay.removeClass('CCW')
            viewerDisplay.addClass('rotate CW')
            viewer.addClass('rotateCW')
        } else {    // reset to default
            viewer.removeClass('rotateCW')
            viewerDisplay.removeClass('rotate CW CCW')
        }
    }
    img.src = url
}

const updateViewerState = () => {
    let {
        url,
        isFav,
        favUrl,
        pages
    } = getState()

    adjustViewerOrientation(url)

    // set viewer image
    $('#image-viewer-display').css('background-image', `url(${url})`)
    setFavoriteButtonState(isFav, favUrl)
    updatePageCounter(pages)
}

const cycleImage = (nextComic, nextPicture) => {
    let {url, pages} = getState()
    let next
    let state
    let np = nextPicture    // TODO: nextPicture vanishes inside else block; why?
    if (pages) {    // comic book
        next = nextComic(pages)
        state = createComicState(url, next, pages)
    } else {        // picture
        let picture = fetchImageAt(url)
        next = np(picture)
        let nextPicture = fetchImageAtPosition(next)
        state = createPictureState(nextPicture)
    }
    setState(state)
    updateViewerState()
}

const showPrevImage = () => {
    cycleImage(
        pages => pages.current === 0 ? pages.total - 1 : --pages.current,
        picture => picture.pos === 0 ? fetchPictureCount() - 1 : --picture.pos
    )
}

const showNextImage = () => {
    cycleImage(
        pages => pages.current >= pages.total - 1 ? 0 : ++pages.current,
        picture => picture.pos >= fetchPictureCount() - 1 ? 0 : ++picture.pos
    )
}

const onShowImageViewer = (id, isComic) => {
    // prevent body scroll
    $('body').css('overflow', 'hidden')
    // display viewer
    $('#image-viewer-container').css('display', 'block')

    if (isComic) {    // comic display
        let comic = fetchComic(id)
        let pages = getComicPages(comic)
        let favUrl = getFavoriteUrl(comic)
        let state = {
            url: pages[0].href,
            isFav: comic.favorite ?? false,
            favUrl,
            pages: {
                current: 0,
                total: pages.length
            },
        }
        setState(state)
        updateViewerState()
    } else { // picture display
        let _picture = fetchImageById(id)
        let picture = _picture.data
        let state = {
            url: getDataUrl(picture),
            isFav: picture.favorite,
            favUrl: getFavoriteUrl(picture),
            pages: null,
        }
        setState(state)
        updateViewerState()
    }
}

const onHideImageViewer = () => {
    $('body').css('overflow', 'revert')
    let container = $('#image-viewer-container')
    container.css('display', 'none')
}

const onFavoriteImage = async () => {
    const favButton = $('#Toggle-image-favorite-button')

    let {favUrl, pages} = getState()
    let isComic = pages !== null

    await toggleFavorite(favUrl, (response) => {
        let {_links, ...rest} = response
        let links = getLinksArray(_links)
        let data = {
            ...rest,
            links
        }

        isComic ? loadComic(data) : loadImage(data)
        response['favorite'] ?
            favButton.addClass('favorite') :
            favButton.removeClass('favorite')
    })
}

export const attachImageHandlers = (image) => {
    const imgCard = $(`#${image.id}`)
    isComic(image) ?
        imgCard.on('click', () => onShowImageViewer(image.id, true)) :
        imgCard.on('click', () => onShowImageViewer(image.id, false))

}

export const attachImageViewerHandlers = () => {
    const  container = $('#image-viewer-container')
    attachImageCycleSwipe(container)

    $('#Image-viewer-close-button').on('click', () => onHideImageViewer())
    $('#Toggle-image-favorite-button').on('click tap', () => onFavoriteImage())
    $(document).on('keydown', (e) => {
        let display = container.css('display')
        if (display && display !== 'none') {
            if (e.key === 'ArrowLeft') showPrevImage()
            if (e.key === 'ArrowRight') showNextImage()
        }
    })
}

export const attachImageCycleSwipe = (element) => {
    element.on('touchstart', (e) => onStartSwipe(e))
    element.on('touchend', (e) => onEndSwipe(e,
        () => showNextImage(),
        () => showPrevImage()))
}
