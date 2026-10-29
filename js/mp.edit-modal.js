import md5 from "md5";
import {getState, setState} from "./data/mp.state";
import {scrollChildIntoView} from "./mp.util";


console.debug('mp.edit-modal.js was picked up')

// HTML elements
// ===================================
const titleEditor = $('#Entity-title')
const tagEditor = $('#Tag-editor')
const tagInput = $('#Tag-input')
const tagList = $('#Tag-list')
const editDialog = $('#Edit-dialog')
const editBody = $('#Edit-dialog-body')
const deleteConfirm = $('#Delete-confirm-body')
const cancelButton = $('#Cancel-button')

// save button
const saveButton = $('#Save-button')
const saveButtonLabel = $('#Save-button span')
const saveSpinner = $('#Save-button .Loading-spinner')

// delete button
const deleteButton = $('#Delete-button')
const cancelDeleteButton = $('#Cancel-delete-button')

// delete confirm button
const confirmDeleteButton = $('#Confirm-delete-button')
const deleteConfirmTitle = $('#Delete-confirm-title')
const deleteButtonLabel = $('#Delete-confirm-label')
const deleteSpinner = $('#Delete-confirm-spinner .Loading-spinner:first-of-type')

// tag suggestions
const suggestionContainer = $('#Tag-suggestion-container')
const tagSuggestions = $('#Tag-suggestions')

// edit modal
// ===================================
const resetTagEditor = () => {
    tagList.html('')
    tagInput.val('')
    loadInterfaceState()
}

const loadInterfaceState = () => {
    const {editedEntity} = getState()
    if (!editedEntity) return
    const {title: editedTitle, tags} = editedEntity

    titleEditor.val(editedTitle)
    deleteConfirmTitle.text(editedTitle)
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

const hideEditDialog = () => {
    editDialog.css('display', 'none')
}

const addTag = (e) => {
    e && e.preventDefault()

    const {editedEntity} = getState()
    const {tags: editedTags} = editedEntity
    let name = tagInput.val()
    let tags = editedTags ?? []

    // check for duplicate
    for (let i = 0; i < tags.length; i++) {
        if (tags[i].name === name) {
            tagInput.val('')
            return
        }
    }

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
    tagInput.focus()
    resetSuggestions()
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

const onSelectTagSuggestion = (value) => {
    suggestionContainer.addClass('hidden')
    tagInput.val(value)
    addTag()
}

const onSearchValueChange = (value) => {
    if (value === '') {
        suggestionContainer.addClass('hidden')
        return
    }

    $.ajax({
        url: '/tags/search',
        data: {q: value},
    }).done(tags => {
        tagSuggestions.html('')

        if (tags && tags.length > 0) {
            suggestionContainer.removeClass('hidden')
            tags.filter(tag => tag.name !== value).forEach(tag => {
                const li = document.createElement('li')
                li.className = 'Tag-suggestion'
                li.innerHTML = tag.name
                li.onclick = (e) => onSelectTagSuggestion($(e.target).text())
                tagSuggestions.append(li)
            })
        }
    })
}

const lockEditModal = (purpose) => {
    saveButton.prop('disabled', true)
    deleteButton.prop('disabled', true)
    tagInput.prop('disabled', true)
    if (purpose === 'delete') {
        deleteButtonLabel.css('display', 'none')
        deleteSpinner.css('display', 'inline-block')
    } else {
        saveButtonLabel.css('display', 'none')
        saveSpinner.css('display', 'inline-block')
    }
}

const unlockEditModal = () => {
    saveButton.prop('disabled', false)
    deleteButton.prop('disabled', false)
    saveButtonLabel.css('display', 'flex')
    saveSpinner.css('display', 'none')
    tagInput.prop('disabled', false)
}

const updateEntity = (link, entity, done) => {
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
        .fail(err => {
            clearEditDialog()
            console.error('failed updating!', err)
        })
}

const onSaveEditDialog = () => {
    const {editedEntity, updateUrl, updateSuccess} = getState()
    lockEditModal('save')
    updateEntity(updateUrl, editedEntity, response => {
        unlockEditModal()
        updateSuccess && updateSuccess(response)
    })
}

const onDeleteEntity = () => {
    deleteConfirm.css('display', 'block')
    editBody.css('display', 'none')
}

const onCancelDelete = () => {
    deleteConfirm.css('display', 'none')
    editBody.css('display', 'flex')
}

const onConfirmDelete = () => {
    const {editedEntity, deleteUrl} = getState()
    console.log('deleting entity', editedEntity, deleteUrl)
    lockEditModal('delete')

    // send delete request
    const request = {
        url: deleteUrl,
        method: 'DELETE',
    }
    $.ajax(request)
        .done(response => {
            console.log('deleted entity', response)
            window.location.reload()
        })
        // TODO: show error message
        .fail(err => {
            console.error('failed deleting!', err.statusText)
            unlockEditModal()
        })
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
    unlockEditModal()
}

let selectedIdx = -1

const setSelectedIndex = (selectedIdx) => {
    // reset unselected options
    tagSuggestions.children().removeClass('selected')

    for (let i = 0; i < tagSuggestions.children().length; i++) {
        if (i === selectedIdx) {
            // update interface
            let suggestion = tagSuggestions.children().eq(i)
            suggestion.addClass('selected')
            tagInput.val(suggestion.text())
            scrollChildIntoView(suggestionContainer, suggestion)
        }
    }
}

const resetSuggestions = () => {
    // reset suggestions
    selectedIdx = -1
    suggestionContainer.addClass('hidden')
}

const handleTagSearchInput = (e) => {
    if (e.key === 'Escape') {
        resetTagEditor()
        resetSuggestions()
    }

    if (!suggestionContainer.hasClass('hidden')) {
        if (e.key === 'ArrowDown' && selectedIdx < tagSuggestions.children().length - 1)
            selectedIdx++
        else if (e.key === 'ArrowUp') {
            if (selectedIdx > 0) selectedIdx--
            else if (selectedIdx === 0) {
                resetSuggestions()
                return
            }
        }

        setSelectedIndex(selectedIdx)
    }
}

export const setupEditModal = () => {
    tagEditor.on('submit', (e) => addTag(e))
    saveButton.on('click', onSaveEditDialog)
    deleteButton.on('click', onDeleteEntity)
    cancelButton.on('click', clearEditDialog)
    cancelDeleteButton.on('click', onCancelDelete)
    confirmDeleteButton.on('click', onConfirmDelete)

    // handle tag editor
    tagInput.on('input', (e) => {
        e.preventDefault()
        const value = e.target.value
        onSearchValueChange(value)
        setState({editedTag: value})
    })
    $(document).on('keydown', e => {
        if (tagInput.is(':focus')) handleTagSearchInput(e)
    })
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
    tagInput.focus()
}
