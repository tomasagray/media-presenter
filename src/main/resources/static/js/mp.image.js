import {onEndSwipe, onStartSwipe, showNextImage, showPrevImage} from "./mp.js";

console.log('mp.image.js was picked up')

export const attachImageCycleSwipe = (element) => {
    let images = element.find('.Display-image')
    element.on('touchstart', (e) => onStartSwipe(e))
    element.on('touchend', (e) => onEndSwipe(e,
        () => showNextImage(images),
        () => showPrevImage(images)))
}

export const cycleImages = (images) => {
    return setInterval(() => showNextImage(images), 1000)
}

export const updatePageCounter = (current, total) => {
    let oneIndex = current + 1
    let text = `${oneIndex} / ${total}`
    $('#Page-counter').text(text)
}

export const onShowImageViewer = (images, selected) => {
    // prevent body scroll
    $('body').css('overflow', 'hidden')

    for (const img of images) {
        let clone = img.cloneNode(true)
        let container = document.createElement('div')
        container.classList.add('Display-image')
        container.appendChild(clone)
        document.getElementById('image-viewer').appendChild(container)
    }

    let container = $('#image-viewer-container')
    container.css('display', 'block')
    attachImageCycleSwipe(container)

    let pageCounter = $('#Page-counter')
    if (selected) { // picture display
        setSelected(selected)
        pageCounter.css('visibility', 'hidden')
    } else {    // comic display
        updatePageCounter(0, images.length)
        pageCounter.css('visibility', 'visible')
        // show first page
        container.find('.Display-image')[0].classList.add('current')
    }
}

const setSelected = (selected) => {
    let images = $('#image-viewer .Display-image')
    images.each((idx, pic) => {
        let src = pic.querySelector('img').src
        src === selected ? pic.classList.add('current') :
            pic.classList.remove('current')
    })
}

export const onHideImageViewer = () => {
    $('body').css('overflow', 'revert')
    let container = document.getElementById('image-viewer-container')
    container.style.display = 'none'
    document.getElementById('image-viewer').innerHTML = ''
}

export const getSiblingImages = () => {
    let cards = document.getElementsByClassName('Picture-card')
    let images = []
    for (let i = 0; i < cards.length; i++) {
        images[i] = cards[i].querySelector('img')
    }
    return images
}

export const hasPages = (image) => {
    let {links} = image
    let pageLink = links.find(link => link.rel.includes('page_'))
    return pageLink && pageLink.href !== null
}

export const getPageImages = (comic) => {
    return comic.links
        .filter(link => link.rel.includes('page_'))
        .map(link => {
            let img = document.createElement('img')
            img.src = link.href
            img.alt = 'Comic page'
            return img
        })
}

export const attachImageHandlers = (image) => {
    if (hasPages(image)) {
        let images = getPageImages(image)
        $(`#${image.id}`).on('click', () => onShowImageViewer(images))
    } else {
        let images = getSiblingImages()
        let selected = image.links.find(link => link.rel === 'data').href
        $(`#${image.id}`).on('click', () => onShowImageViewer(images, selected))
    }
}

export const attachImageViewerHandlers = () => {
    document.addEventListener('keydown', (e) => {
        let container = document.querySelector('#image-viewer-container')
        let display = container.style.display
        if (display && display !== 'none') {
            let images = container.querySelectorAll('.Display-image')
            if (e.key === 'ArrowLeft') {
                showPrevImage(images)
            }
            if (e.key === 'ArrowRight') {
                showNextImage(images)
            }
        }
    })
}