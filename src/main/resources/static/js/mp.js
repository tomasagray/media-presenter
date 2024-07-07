console.log('mp.js was picked up')


const MIN_SWIPE_PX = 10

export const attachVideoCardHandlers = (video) => {
    let {id} = video
    let element = $('#' + id)
    // attach event handlers
    element.on('click', () => showVideoPlayer(video))
    attachImageCycleSwipe(element)

    let images = element.find('.Display-image')
    let timer
    element.on('mouseenter', () => timer = cycleImages(images))
    element.on('mouseleave', () => clearInterval(timer))
}

export const showVideoPlayer = (video) => {
    attachFavoriteButtonBehavior(video)

    $('.Footer-menu-container').css('display', 'none')
    $('#Video-player-container').css('display', 'block')

    let player = $('#Video-player')
    let url = getVideoLink(video.links)
    player.attr('src', url)
    player[0].load()
}

export const attachFavoriteButtonBehavior = (entity) => {
    let {favorite} = entity
    let $favoriteButtons = $('.Favorite-button')
    $favoriteButtons.each((idx, favoriteButton) => {
        favorite ? $(favoriteButton).addClass('favorite')
            : $(favoriteButton).removeClass('favorite')
        $(favoriteButton).unbind().click(() => toggleFavorite(entity))
    })
}

const toggleFavorite = async (entity) => {
    const $favoriteButton = $('.Favorite-button')
    $favoriteButton.attr('enabled', false)
    $favoriteButton.removeClass('favorite')
    $favoriteButton.addClass('loading')

    let link = entity['links'].find(link => link.rel === 'favorite').href
    await $.ajax({
        url: link,
        method: 'PATCH',
    })
        .done((video) => {
            video['favorite'] ?
                $favoriteButton.addClass('favorite') :
                $favoriteButton.removeClass('favorite')
        })
        .fail((err) => {
            console.error('favoriting failed!', err)
        })
        .always(() => {
            $favoriteButton.removeClass('loading')
            $favoriteButton.attr('enabled', true)
        })
}

export const hideVideoPlayer = () => {
    let player = $('#Video-player')
    player.attr('src', null)
    player[0].load()
    $('.Footer-menu-container').css('display', 'flex')
    $('#Video-player-container').css('display', 'none')
}

export const getVideoLink = (links) => links?.find(link => link.rel === 'data')?.href

// TODO: use jQuery selectors throughout
export const attachImageCycleSwipe = (element) => {
    let images = element.find('.Display-image')
    element.on('touchstart', (e) => onStartSwipe(e))
    element.on('touchend', (e) => onEndSwipe(e,
        () => showNextImage(images),
        () => showPrevImage(images)))
    // show first image
    images[0].classList.add('current')
}

export const cycleImages = (images) => {
    return setInterval(() => showNextImage(images), 1000)
}

const updatePageCounter = (current, total) => {
    let oneIndex = current + 1
    let text = `${oneIndex} / ${total}`
    $('#Page-counter').text(text)
}

export const showPrevImage = (images) => {
    let end = images.length - 1
    let current = 0
    for (let j = end; j >= 0; j--) {
        if (images[j].classList.contains('current')) {
            images[j].classList.remove('current')
            if (j > 0) {
                current = j - 1
            } else {
                current = end
            }
        }
    }
    images[current].classList.add('current')
    updatePageCounter(current, images.length)
}

export const showNextImage = (images) => {
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
    updatePageCounter(current, images.length)
}

let touchStart = 0, touchEnd = 0
export const onStartSwipe = (e) => {
    touchStart = e.changedTouches[0].screenX
}

export const onEndSwipe = (e, onSwipeLeft, onSwipeRight) => {
    touchEnd = e.changedTouches[0].screenX
    let swipe = Math.abs(touchStart - touchEnd)
    if (swipe > MIN_SWIPE_PX) {
        if (touchStart > touchEnd) onSwipeLeft()
        else onSwipeRight()
    }
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

    let pageCounter = $('#Page-counter')
    if (selected) { // picture display
        setSelected(selected)
        pageCounter.css('visibility', 'hidden')
    } else {    // comic display
        updatePageCounter(0, images.length)
        pageCounter.css('visibility', 'visible')
    }

    let container = $('#image-viewer-container')
    container.css('display', 'block')
    attachImageCycleSwipe(container)
}

const setSelected = (selected) => {
    let images = document.querySelectorAll('#image-viewer .Display-image')
    images.forEach(img => {
        let src = img.querySelector('img').src
        if (src === selected) {
            img.classList.add('current')
        } else {
            img.classList.remove('current')
        }
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

let listener = null
export const onShowSearchModal = () => {
    $('#search-modal-container').css('display', 'block')
    $('#search-form').focus()
    setTimeout(() => {  // prevent race condition
        onClickOutside('#search-modal')
        document.addEventListener('click', listener)
    }, 50)
}

const onClickOutside = (selector) => {
    listener = (e) => {
        const closest = e.target.closest(selector)
        if (closest === null && $(selector).is(':visible')) {
            onHideSearchModal()
        }
    }
}

export const onHideSearchModal = () => {
    $('#search-modal-container').css('display', 'none')
    document.removeEventListener('click', listener)
}
