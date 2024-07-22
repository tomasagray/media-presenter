import {onEndSwipe, onStartSwipe, toggleFavorite} from "./mp.js";


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

const showVideoPlayer = (video) => {
    attachFavoriteButtonBehavior(video)

    $('.Footer-menu-container').css('display', 'none')
    $('#Video-player-container').css('display', 'block')

    let player = $('#Video-player')
    let url = getVideoLink(video.links)
    player.attr('src', url)
    player[0].load()
}

export const hideVideoPlayer = () => {
    let player = $('#Video-player')
    player.attr('src', null)
    player[0].load()
    $('.Footer-menu-container').css('display', 'flex')
    $('#Video-player-container').css('display', 'none')
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
