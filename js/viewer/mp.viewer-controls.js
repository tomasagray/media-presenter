import {getViewportDimensions} from '../mp.util'


console.debug('mp.viewer-controls.js was picked up')

const IDLE_SECONDS = 5

// UI components
const viewerControls = $('.Viewer-controls-container')
const viewerDisplay = $('#image-viewer-display')
const closeButton = $('#Close-viewer-button')
const favButton = $('#Toggle-favorite-button')

let idleTimer, displayStyle
const hideViewerControls = () => {
    displayStyle = viewerControls.css('display')
    viewerControls.fadeOut('fast')
}

export const resetIdleTimer = () => {
    clearTimeout(idleTimer)
    idleTimer = setTimeout(hideViewerControls, IDLE_SECONDS * 1_000)
    viewerControls.stop().css({
        'opacity': 1,
        'display': displayStyle ?? 'flex',
    })
}

export const adjustViewerOrientation = (url) => {
    let img = new Image()
    img.onload = () => {
        let {width: vw, height: vh} = getViewportDimensions()

        if (vw > vh && img.height > img.width) {    // landscape mode
            viewerDisplay.removeClass('CW')
            viewerDisplay.addClass('rotate CCW')

            // TODO: move this to @media rule?
            // fix for iPads
            if (navigator.platform.includes('Mac')) {
                $('#image-viewer-display').css('transform', 'translate(15vw, -15%) rotate(-90deg)')
            }
        } else if (vh > vw && img.width > img.height) {    // portrait mode
            viewerDisplay.removeClass('CCW')
            viewerDisplay.addClass('rotate CW')
        } else {    // reset to default
            viewerDisplay.removeClass('rotate CW CCW')

            // undo fix for iPads
            if (navigator.platform.includes('Mac')) {
                $('#image-viewer-display').css('transform', '')
            }
        }
    }
    img.src = url
}

export const setCloseButtonHandler = (onClose) => closeButton.unbind().click(onClose)

export const setFavButtonHandler = (onToggle) => favButton.unbind().click(onToggle)
