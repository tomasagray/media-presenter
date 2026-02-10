console.debug('mp.util.js was picked up')

const MIN_SWIPE_PX = 10

// Utility methods
// ===================================
export const formatSeconds = (seconds) => {
    const date = new Date(null)
    date.setSeconds(seconds)

    let start = seconds >= 3600 ? 11 : 14
    return date.toISOString().slice(start, 19)
}

export const getLinkUrl = (entity, rel) => entity.links.find(link => link.rel === rel)?.href

export const getLinksArray = (_links) => {
    return Object.entries(_links).map((link) => ({
        rel: link[0],
        href: link[1].href
    }))
}

export const fetchFromRepoAt = (repo, pos) => {
    let i = 0, requested = null
    let values = repo.values()
    while (i <= pos) {
        requested = values.next().value
        i++
    }
    return requested
}

// input handlers
// ===================================
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

export const getViewportDimensions = () => {
    let width = Math.max(document.documentElement.clientWidth || 0, window.innerWidth || 0)
    let height = Math.max(document.documentElement.clientHeight || 0, window.innerHeight || 0)
    return {
        width,
        height,
    }
}
