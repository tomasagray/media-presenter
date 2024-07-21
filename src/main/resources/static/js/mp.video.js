import {toggleFavorite} from "./mp.js";
import {attachImageCycleSwipe} from "./mp.image.js";


console.log('mp.video.js was picked up')


const getVideoLink = (links) => links?.find(link => link.rel === 'data')?.href

const showNextImage = (images) => {
    let current = 1
    for (let i = 0; i < images.length; i++) {
        if (images[i].classList.contains('current')) {
            images[i].classList.remove('current')
            if (i === images.length - 1) {
                current = 0
            } else {
                current = i + 1
            }
        }
    }
    images[current].classList.add('current')
}

const cycleImages = (images) => setInterval(() => showNextImage(images), 1000)

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

const showVideoPlayer = (video) => {
    attachFavoriteButtonBehavior(video)

    $('.Footer-menu-container').css('display', 'none')
    $('#Video-player-container').css('display', 'block')

    let player = $('#Video-player')
    let url = getVideoLink(video.links)
    player.attr('src', url)
    player[0].load()
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
    element.on('mouseenter', () => timer = cycleImages(images))
    element.on('mouseleave', () => clearInterval(timer))
}

export const hideVideoPlayer = () => {
    let player = $('#Video-player')
    player.attr('src', null)
    player[0].load()
    $('.Footer-menu-container').css('display', 'flex')
    $('#Video-player-container').css('display', 'none')
}
