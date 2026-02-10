import {toggleFavorite} from "../mp.endpoints.js";
import {fetchImageAt, fetchImageAtPosition, fetchImageById, fetchPictureCount, loadImage} from "../data/mp.image_repo.js";
import {fetchComic, fetchComicForPage, getComicPageLinks, isComic, loadComic} from "../data/mp.comic_repo.js";
import {getState, setState} from "../data/mp.state.js";
import {getLinksArray, getLinkUrl, getViewportDimensions, onEndSwipe, onStartSwipe} from "../mp.util";


console.debug('mp.image.js was picked up')

// UI components
const viewerContainer = $('#Viewer-container')
const closeButton = $('#Close-viewer-button')
const favButton = $('#Toggle-favorite-button')
const pageCounter = $('#Page-counter')
const imageViewerContainer = $('#image-viewer-container')
const imageViewer = $('#image-viewer')
const viewerDisplay = $('#image-viewer-display')
const footerMenu = $('#Footer-menu-container')

const setFavoriteButtonState = (isFav) => {
    isFav ? favButton.addClass('favorite') :
        favButton.removeClass('favorite')
}

const updatePageCounter = (pages) => {
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
    let comicPages = getComicPageLinks(comic)
    let page = comicPages[prev].href
    return {
        url: page,
        isFav: comic.favorite,
        favLink: getLinkUrl(comic, 'favorite'),
        pages: {
            current: prev,
            total: pages.total
        }
    }
}

const createPictureState = (prevPicture) => {
    return {
        url: getLinkUrl(prevPicture, 'data'),
        isFav: prevPicture.favorite,
        favUrl: getLinkUrl(prevPicture, 'favorite'),
        pages: null,
    }
}

$(window).resize(() => {
    let display = imageViewerContainer.css('display')
    if (display === 'block') updateViewerState()
})

const adjustViewerOrientation = (url) => {
    let img = new Image()
    img.onload = () => {
        let {width: vw, height: vh} = getViewportDimensions()

        if (vw > vh && img.height > img.width) {    // landscape mode
            viewerDisplay.removeClass('CW')
            viewerDisplay.addClass('rotate CCW')

            // TODO: move this to @media rule?
            // fix for iPads
            if (navigator.platform.includes('Mac')) {
                $('#image-viewer-display').css('transform', 'translate(15vw, -15%) rotate(-90deg)')
            }
        } else if (vh > vw && img.width > img.height) {    // portrait mode
            viewerDisplay.removeClass('CCW')
            viewerDisplay.addClass('rotate CW')
        } else {    // reset to default
            viewerDisplay.removeClass('rotate CW CCW')

            // undo fix for iPads
            if (navigator.platform.includes('Mac')) {
                $('#image-viewer-display').css('transform', '')
            }
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
    viewerDisplay.css('background-image', `url(${url})`)
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

const getComicState = (comic) => {
    let pages = getComicPageLinks(comic)
    return {
        id: comic.id,
        title: comic.title,
        tags: comic.tags,
        url: pages[0].href,
        pages: {
            current: 0,
            total: pages.length
        },
        isFav: comic.favorite ?? false,
        favUrl: getLinkUrl(comic, 'favorite'),
        updateUrl: getLinkUrl(comic, 'update'),
        updateSuccess: onUpdateComic,
    }
}

const getPictureState = (picture) => {
    return {
        id: picture.id,
        title: picture.title,
        tags: picture.tags,
        isFav: picture.favorite,
        pages: null,
        url: getLinkUrl(picture, 'data'),
        favUrl: getLinkUrl(picture, 'favorite'),
        updateUrl: getLinkUrl(picture, 'update'),
        updateSuccess: onUpdatePicture,
    }
}

const onUpdatePicture = (updated) => {
    loadImage(normalizeResponse(updated))
    loadPictureToInterface(updated.id)
}

const onUpdateComic = (updated) => {
    loadComic(normalizeResponse(updated))
    loadComicToInterface(updated.id)
}

const loadPictureToInterface = (id) => {
    let _picture = fetchImageById(id)
    let picture = _picture.data
    let state = getPictureState(picture)
    setState(state)
    updateViewerState()
}

const loadComicToInterface = (id) => {
    pageCounter.css('display', 'block')
    let comic = fetchComic(id)
    let state = getComicState(comic)
    setState(state)
    updateViewerState()
}

const onShowImageViewer = (id, isComic) => {
    // prevent body scroll
    $('body').css('overflow', 'hidden')

    // menu handlers
    closeButton.on('click', () => onHideImageViewer())
    favButton.on('click', () => onFavoriteImage())

    // display viewer
    viewerContainer.css('display', 'block')
    imageViewerContainer.css('display', 'block')
    imageViewer.css('display', 'block')
    footerMenu.css('display', 'none')

    isComic ? loadComicToInterface(id) :
        loadPictureToInterface(id)
}

const onHideImageViewer = () => {
    $('body').css('overflow', 'revert')
    viewerContainer.css('display', 'none')
    imageViewerContainer.css('display', 'none')
    footerMenu.css('display', 'flex')
    imageViewer.css('display', 'none')
}

const normalizeResponse = (response) => {
    let {_links, ...rest} = response
    let links = getLinksArray(_links)
    return {
        ...rest,
        links
    }
}

const onFavoriteImage = async () => {
    let {favUrl, pages} = getState()
    let isComic = pages !== null

    await toggleFavorite(favUrl, (response) => {
        let data = normalizeResponse(response)
        isComic ? loadComic(data) : loadImage(data)
        response.favorite ?
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
    const container = imageViewerContainer
    attachImageCycleSwipe(container)

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
