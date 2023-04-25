console.log('mp.js was picked up')


const MIN_SWIPE_PX = 10

export const showVideoPlayer = (url) => () => {
    $('#Video-player-container').css('display', 'block')
    let player = $('#Video-player')
    player.attr('src', url)
    player[0].load()
}

export const hideVideoPlayer = () => {
    let player = $('#Video-player')
    player.attr('src', null)
    player[0].load()
    $('#Video-player-container').css('display', 'none')
}

export const getVideoLink = (links) => links?.find(link => link.rel === 'data')?.href

export const attachSwipe = (element, onclick) => {
    let images = element.querySelectorAll('.Display-image')
    console.log('swipe images', images)
    element.onclick = onclick
    let timer
    element.onmouseenter = () => timer = cycleImages(images)
    element.onmouseleave = () => clearInterval(timer)
    element.ontouchstart = (e) => onStartSwipe(e)
    element.ontouchend = (e) => onEndSwipe(e,
        () => showPrevImage(images),
        () => showNextImage(images))
    // show first image
    element.querySelector('.Display-image').classList.add('current')
}

export const cycleImages = (images) => {
    return setInterval(() => showNextImage(images), 1000)
}

export const showPrevImage = (images) => {
    let end = images.length - 1
    for (let j = end; j >= 0; j--) {
        if (images[j].classList.contains('current')) {
            images[j].classList.remove('current')
            if (j > 0) {
                images[j - 1].classList.add('current')
            } else {
                images[end].classList.add('current')
            }
            return
        }
    }
    images[0].classList.add('current')
}

export const showNextImage = (images) => {
    for (let i = 0; i < images.length; i++) {
        if (images[i].classList.contains('current')) {
            images[i].classList.remove('current')
            if (i === images.length - 1) {
                images[0].classList.add('current')
            } else {
                images[i + 1].classList.add('current');
            }
            return
        }
    }
    // 'current' was not set, set the first thumb
    images[0].classList.add('current')
}

let touchStart = 0, touchEnd = 0
export const onStartSwipe = (e) => {
    touchStart = e.changedTouches[0].screenX
}

export const onEndSwipe = (e, onSwipeLeft, onSwipeRight) => {
    touchEnd = e.changedTouches[0].screenX
    let swipe = Math.abs(touchStart - touchEnd)
    if (swipe > MIN_SWIPE_PX) {
        if (touchStart > touchEnd) {
            onSwipeLeft();
        } else {
            onSwipeRight();
        }
    }
}

export const onShowImageViewer = (images) => {
    for (const img of images) {
        let clone = img.cloneNode(true)
        let container = document.createElement('div')
        container.classList.add('Display-image')
        container.appendChild(clone)
        document.getElementById('image-viewer').appendChild(container)
    }
    let container = document.getElementById('image-viewer-container')
    container.style.display = 'block'
    attachSwipe(container, null)
}

export const onHideImageViewer = () => {
    let container = document.getElementById('image-viewer-container')
    container.style.display = 'none'
    document.getElementById('image-viewer').innerHTML = ''
}