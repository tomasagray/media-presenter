import {setState} from "../data/mp.state.js";
import {fetchVideoById, loadVideo} from "../data/mp.video_repo.js";
import {formatSeconds, getLinkUrl, getViewportDimensions, onEndSwipe, onStartSwipe} from "../mp.util";
import {toggleFavorite} from "../mp.endpoints";


console.debug('mp.video.js was picked up')

const idleSeconds = 5
const viewerControls = $('.Viewer-controls-container')

const playIconSrc = '/img/icon/play/play_32.png'
const pauseIconSrc = '/img/icon/pause/pause_32.png'
// UI components
const footerMenu = $('#Footer-menu-container')
const playerContainer = $('#Video-player-container')
const player = $('#Video-player')
const playIndicator = $('#Video-play-indicator')
const viewerContainer = $('#Viewer-container')
const closeViewerButton = $('#Close-viewer-button')
const favoriteButton = $('#Toggle-favorite-button')
// video player controls
const playIndicatorButton = $('#Video-play-indicator-button')
const currentTimeDisplay = $('#Video-current-time')
const durationDisplay = $('#Video-duration')
const slider = $('#Video-time-slider-control')
const timeIndicators = $('.Video-time-indicator')
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
    let display = playerContainer.css('display')
    if (display === 'block') adjustVideoPlayerOrientation()
})

let videoWidth, videoHeight = 0

const adjustVideoPlayerOrientation = () => {
    let {width: vw, height: vh} = getViewportDimensions()

    if (vw > vh && videoHeight > videoWidth) {  // landscape
        player.removeClass('CW').addClass('rotate CCW')
    } else if (vh > vw && videoWidth > videoHeight) {   // portrait
        player.removeClass('CCW').addClass('rotate CW')
    } else {    // default
        player.removeClass('rotate CW CCW')
    }
}

const onUpdateVideo = (updated) => {
    loadVideo(updated)
    setState({
        title: updated.title,
        tags: updated.tags,
    })
}

const hideVideoPlayer = () => {
    viewerContainer.css('display', 'none')
    footerMenu.css('display', 'flex')
    playerContainer.css('display', 'none')

    $('body').css('overflow', 'revert')
    timeIndicators.text('--:--')
    player.attr('src', null)
    player[0].load()
}

const showVideoPlayer = (id) => {
    let video = fetchVideoById(id).data
    $('body').css('overflow', 'hidden')

    closeViewerButton.on('click', hideVideoPlayer)
    attachFavoriteButtonBehavior(video)

    setState({
        id: video.id,
        title: video.title,
        tags: video.tags,
        updateUrl: getLinkUrl(video, 'update'),
        updateSuccess: onUpdateVideo,
    })

    footerMenu.css('display', 'none')
    viewerContainer.css('display', 'block')
    playerContainer.css('display', 'block')

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
    let link = getLinkUrl(entity, 'favorite')

    favorite ? favoriteButton.addClass('favorite')
        : favoriteButton.removeClass('favorite')
    favoriteButton.unbind().click(() => toggleFavorite(link, (video) => {
        video.favorite ?
            favoriteButton.addClass('favorite') :
            favoriteButton.removeClass('favorite')
    }))
}

const attachImageCycleSwipe = (element) => {
    const images = element.find('.Display-image')
    element.on('touchstart', (e) => onStartSwipe(e))
    element.on('touchend', (e) => onEndSwipe(e,
        () => showNextImage(images),
        () => showPrevImage(images)))
}

const playVideo = (player) => {
    player.trigger('play')
    playIndicator.attr('src', pauseIconSrc)
}

const pauseVideo = (player) => {
    player.trigger('pause')
    playIndicator.attr('src', playIconSrc)
}

export const pauseUnpauseVideo = (player) => {
    player[0].paused ?
        playVideo(player) :
        pauseVideo(player)
}

export const setupVideoPlayer = () => {
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
    player.on('timeupdate', () => {
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
    playIndicatorButton.on('click', () => pauseUnpauseVideo(player))
}

export const attachVideoCardHandlers = (id) => {
    let element = $('#' + id)
    // attach event handlers
    element.on('click', () => showVideoPlayer(id))
    attachImageCycleSwipe(element)

    let images = element.find('.Display-image')
    images[0].classList.add('current')  // show first thumbnail
    let timer
    element.on('mouseenter', () => timer = autoCycleImages(images))
    element.on('mouseleave', () => clearInterval(timer))
}

export const skipVideoTime = (skip) => {
    let targetTime = player[0].currentTime + skip
    if (targetTime <= 0 || targetTime >= player[0].duration)
        targetTime = 0

    console.log('skipping video to ', targetTime)
    player[0].currentTime = targetTime
}

let idleTimer, displayStyle
const hideViewerControls = () => {
    displayStyle = viewerControls.css('display')
    viewerControls.fadeOut('fast')
}

export const resetIdleTimer = () => {
    clearTimeout(idleTimer)
    idleTimer = setTimeout(hideViewerControls, idleSeconds * 1_000)
    viewerControls.stop().css({
        'opacity': 1,
        'display': displayStyle ?? 'flex',
    })
}
