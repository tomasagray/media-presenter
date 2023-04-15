console.log('mp.js was picked up')

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

export const cycleThumbs = (thumbs) => {
    return setInterval(() => showNextThumb(thumbs), 1000)
}

export const showPrevThumb = (thumbs) => {
    let end = thumbs.length - 1
    for (let j = end; j >= 0; j--) {
        if (thumbs[j].classList.contains('current')) {
            thumbs[j].classList.remove('current')
            if (j > 0) {
                thumbs[j - 1].classList.add('current')
            } else {
                thumbs[end].classList.add('current')
            }
            return
        }
    }
    thumbs[0].classList.add('current')
}

export const showNextThumb = (thumbs) => {
    for (let i = 0; i < thumbs.length; i++) {
        if (thumbs[i].classList.contains('current')) {
            thumbs[i].classList.remove('current')
            if (i === thumbs.length - 1) {
                thumbs[0].classList.add('current')
            } else {
                thumbs[i + 1].classList.add('current');
            }
            return
        }
    }
    // 'current' was not set, set the first thumb
    thumbs[0].classList.add('current')
}

let touchStart = 0, touchEnd = 0
export const onStartSwipe = (e) => {
    touchStart = e.changedTouches[0].screenX
}

export const onEndSwipe = (e, thumbs) => {
    touchEnd = e.changedTouches[0].screenX
    if (touchStart > touchEnd) {
        showPrevThumb(thumbs)
    } else {
        showNextThumb(thumbs)
    }
}