console.log('mp.js was picked up')

const MIN_SWIPE_PX = 10

// TODO: use jQuery selectors throughout
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

export const setupSelectedNavItem = () => {
    let url = window.location.href
    let match = url.match(/home|videos|pictures|comics/)
    if (match) {
        let key = match[0]
        let selector = `${key}-link`
        document.getElementById(selector).innerHTML =
            `<img alt="${key}" src="/img/icon/${key}_selected/${key}_selected_64.png" />`
    } else if (url.match(/favorites/)) {
        $('#favorites-link').html(
            '<img alt="Favorites" src="/img/icon/favorite/favorite_64.png" />'
        )
    } else {
        $('#home-link').html(
            '<img alt="Home" src="/img/icon/home_selected/home_selected_64.png" />'
        )
    }
}

export const toggleFavorite = async (link, done) => {
    const favButton = $('.Favorite-button')
    favButton.attr('enabled', false)
    favButton.removeClass('favorite')
    favButton.addClass('loading')

    let request = {
        url: link,
        method: 'PATCH',
    }
    await $.ajax(request)
        .done((response) => done && done(response))
        .fail((err) => console.error('favoriting failed!', err))
        .always(() => {
            favButton.removeClass('loading')
            favButton.attr('enabled', true)
        })
}
