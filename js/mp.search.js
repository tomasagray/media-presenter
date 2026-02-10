console.debug('mp.search.js was picked up')

let listener = null
const onClickOutside = (selector) => {
    listener = (e) => {
        const closest = e.target.closest(selector)
        if (closest === null && $(selector).is(':visible')) {
            onHideSearchModal()
        }
    }
}

export const onShowSearchModal = () => {
    $('#search-modal').css('display', 'flex')
    $('#search-form-input').focus()
    setTimeout(() => {  // prevent race condition
        onClickOutside('.Search-form-container')
        document.addEventListener('click', listener)
    }, 50)
}

export const onHideSearchModal = () => {
    $('#search-modal').css('display', 'none')
    document.removeEventListener('click', listener)
}
