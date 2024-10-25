import {clearState, getState, setState} from "./mp.state.js";


console.log('mp.js was picked up')


const MIN_SWIPE_PX = 10

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
    $('#search-modal').css('display', 'flex')
    $('#search-form').focus()
    setTimeout(() => {  // prevent race condition
        onClickOutside('.Search-form-container')
        document.addEventListener('click', listener)
    }, 50)
}

export const onHideSearchModal = () => {
    $('#search-modal').css('display', 'none')
    document.removeEventListener('click', listener)
}

const onClickOutside = (selector) => {
    listener = (e) => {
        const closest = e.target.closest(selector)
        if (closest === null && $(selector).is(':visible')) {
            onHideSearchModal()
        }
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

// Endpoints
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

export const updateEntity = (link, entity, done) => {
    console.log('updating entity at', link)

    const request = {
        url: link,
        method: 'PATCH',
        contentType: 'application/json',
        data: JSON.stringify(entity),
    }
    $.ajax(request)
        .done(response => {
            clearEditDialog()
            done && done(response)
        })
        .fail(err => console.error('failed updating!', err))
}

export const getViewportDimensions = () => {
    let width = Math.max(document.documentElement.clientWidth || 0, window.innerWidth || 0)
    let height = Math.max(document.documentElement.clientHeight || 0, window.innerHeight || 0)
    return {
        width,
        height,
    }
}

const idleSeconds = 5
let idleTimer, displayStyle
const viewerControls = $('#Viewer-controls-container')
const titleEditor = $('#Entity-title')
const tagInput = $('#Tag-input')
const tagList = $('#Tag-list')
const editDialog = $('#Edit-dialog')

// save button
const saveButton = $('#Save-button')
const saveButtonLabel = $('#Save-button span')
const saveSpinner = $('#Save-button .Loading-spinner')

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

const loadInterfaceState = () => {
    const {editedEntity} = getState()
    if (!editedEntity) return
    const {title: editedTitle, tags} = editedEntity

    titleEditor.val(editedTitle)
    tags?.forEach(tag => {
        let {tagId, name} = tag
        const element = $(document.createElement('button'))
        element.attr('id', tagId)
        element.addClass('Entity-tag')
        element.text(name)
        element.on('click', () => deleteTag(tagId))
        tagList.append(element)
    })
}

const resetTagEditor = () => {
    tagList.html('')
    tagInput.val('')
    loadInterfaceState()
}

export const showEditDialog = () => {
    // make edit copy
    const state = getState()
    setState({
        editedEntity: {
            id: state.id,
            title: state.title,
            tags: state.tags,
        }
    })

    loadInterfaceState()
    editDialog.css('display', 'flex')
}

const hideEditDialog = () => {
    editDialog.css('display', 'none')
}

export const addTag = () => {
    const {editedEntity} = getState()
    const {tags: editedTags} = editedEntity
    let name = tagInput.val()
    let tags = editedTags ?? []

    setState({
        editedEntity: {
            ...editedEntity,
            tags: [
                ...tags,
                {
                    name,
                    tagId: md5(name),
                },
            ],
        },
    })

    // reset form
    tagInput.val('')
    resetTagEditor()
}

const deleteTag = (id) => {
    const {editedEntity} = getState()
    const {tags} = editedEntity
    const updated = tags.filter(tag => tag.tagId !== id)
    setState({
        editedEntity: {
            ...editedEntity,
            tags: updated,
        }
    })
    resetTagEditor()
}

export const clearEditDialog = () => {
    hideEditDialog()
    setState({
        editedEntity: {
            title: null,
            tags: [],
        },
        editedTag: '',
    })
    resetTagEditor()
}

const lockEditDialog = () => {
    saveButton.prop('disabled', true)
    tagInput.prop('disabled', true)
    saveButtonLabel.css('display', 'none')
    saveSpinner.css('display', 'inline-block')
}

const unlockEditModal = () => {
    saveButton.prop('disabled', false)
    saveButtonLabel.css('display', 'flex')
    saveSpinner.css('display', 'none')
    tagInput.prop('disabled', false)
}

export const onSaveEditDialog = () => {
    const {editedEntity, updateUrl, updateSuccess} = getState()
    lockEditDialog()
    updateEntity(updateUrl, editedEntity, response => {
        unlockEditModal()
        updateSuccess && updateSuccess(response)
    })
}

// Utility methods
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