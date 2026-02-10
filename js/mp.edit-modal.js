import {getState, setState} from "./data/mp.state";
import md5 from "md5";


console.debug('mp.edit-modal.js was picked up')

// html elements
// ===================================
const titleEditor = $('#Entity-title')
const tagInput = $('#Tag-input')
const tagList = $('#Tag-list')
const editDialog = $('#Edit-dialog')

const saveButton = $('#Save-button')
const saveButtonLabel = $('#Save-button span')
const saveSpinner = $('#Save-button .Loading-spinner')
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

export const onSearchValueChange = (value) => {
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

export const onSaveEditDialog = () => {
    const {editedEntity, updateUrl, updateSuccess} = getState()
    lockEditDialog()
    updateEntity(updateUrl, editedEntity, response => {
        unlockEditModal()
        updateSuccess && updateSuccess(response)
    })
}
