import {formatSeconds, getViewportDimensions, onEndSwipe, onStartSwipe, toggleFavorite} from "./mp.js";


console.log('mp.video.js was picked up')


const getVideoLink = (links) => links?.find(link => link.rel === 'data')?.href

const cycleImages = (images, getNext) => {
    let current = 0
    for (let i = 0; i < images.length; i++) {
        if (images[i].classList.contains('current')) {
            images[i].classList.remove('current')
            current = getNext(i, images.length)
            break
        }
    }
    images[current].classList.add('current')
}

const showNextImage = (images) =>
    cycleImages(images, (current, total) => current === total - 1 ? 0 : current + 1)

const showPrevImage = (images) =>
    cycleImages(images, (current, total) => current === 0 ? total - 1 : current - 1)

const autoCycleImages = (images) => setInterval(() => showNextImage(images), 1000)

$(window).resize(() => {
    let display = $('#Video-player-container').css('display')
    if (display === 'block') adjustVideoPlayerOrientation()
})

let videoWidth, videoHeight = 0

const adjustVideoPlayerOrientation = () => {
    const player = $('#Video-player')
    let {width: vw, height: vh} = getViewportDimensions()
    if (vw > vh && videoHeight > videoWidth) {  // landscape
        player.removeClass('CW')
        player.addClass('rotate CCW')
    } else if (vh > vw && videoWidth > videoHeight) {   // portrait
        player.removeClass('CCW')
        player.addClass('rotate CW')
    } else {
        player.removeClass('rotate CW CCW')
    }
}

const hideVideoPlayer = () => {
    $('body').css('overflow', 'revert')
    let player = $('#Video-player')
    player.attr('src', null)
    player[0].load()
    $('.Footer-menu-container').css('display', 'flex')
    $('#Video-player-container').css('display', 'none')
}

const showVideoPlayer = (video) => {
    $('body').css('overflow', 'hidden')
    $('.Viewer-button.close').on('click', hideVideoPlayer)
    attachFavoriteButtonBehavior(video)

    $('.Footer-menu-container').css('display', 'none')
    $('#Video-player-container').css('display', 'block')

    let player = $('#Video-player')
    player.on('loadedmetadata', (e) => {
        videoWidth = e.target.videoWidth
        videoHeight = e.target.videoHeight
        adjustVideoPlayerOrientation()
    })
    let url = getVideoLink(video.links)
    player.attr('src', url)
    player[0].load()
}

const attachFavoriteButtonBehavior = (entity) => {
    let {favorite} = entity
    let $favoriteButton = $('#Toggle-video-favorite-button')
    let link = entity['links'].find(link => link.rel === 'favorite').href

    favorite ? $favoriteButton.addClass('favorite')
        : $favoriteButton.removeClass('favorite')
    $favoriteButton.unbind().click(() => toggleFavorite(link, (video) => {
        video['favorite'] ?
            $favoriteButton.addClass('favorite') :
            $favoriteButton.removeClass('favorite')
    }))
}

const attachImageCycleSwipe = (element) => {
    const images = element.find('.Display-image')
    element.on('touchstart', (e) => onStartSwipe(e))
    element.on('touchend', (e) => onEndSwipe(e,
        () => showNextImage(images),
        () => showPrevImage(images)))
}

const pauseUnpauseVideo = (player) => {
    player[0].paused ?
        player.trigger('play') :
        player.trigger('pause')
}

export const setupVideoPlayer = () => {
    const player = $('#Video-player')
    const currentTimeDisplay = $('#Video-current-time')
    const durationDisplay = $('#Video-duration')
    const slider = $('#Video-time-slider-control')

    // setup tracking bar
    slider.slider({
        range: 'min',
        min: 0,
        max: 100,
        classes: {
            'ui-slider': 'Video-time-slider',
            'ui-slider-handle': 'Video-time-slider-handle',
            'ui-slider-range': 'Video-time-range',
        },
        slide: (e, ui) => {
            const video = player[0]
            video.currentTime = video.duration * (ui.value / 100)
        }
    })

    // handle video player events
    player.on('loadedmetadata', () => {
        durationDisplay.text('0:00')
        let duration = Math.round(player[0].duration)
        durationDisplay.text(formatSeconds(duration))
    })
    player.on('timeupdate', (e) => {
        const currentTime = player[0].currentTime
        const progress = (currentTime / player[0].duration) * 100

        let timestamp = Math.round(currentTime)
        currentTimeDisplay.text(formatSeconds(timestamp))
        slider.slider('value', progress)
    })
    player.on('click', () => pauseUnpauseVideo(player))
    $(document).on('keyup', (e) => {
        if (e.keyCode === 32) pauseUnpauseVideo(player)
    })
}

export const attachVideoCardHandlers = (video) => {
    let {id} = video
    let element = $('#' + id)
    // attach event handlers
    element.on('click', () => showVideoPlayer(video))
    attachImageCycleSwipe(element)

    let images = element.find('.Display-image')
    images[0].classList.add('current')  // show first thumbnail
    let timer
    element.on('mouseenter', () => timer = autoCycleImages(images))
    element.on('mouseleave', () => clearInterval(timer))
}
