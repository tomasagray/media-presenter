console.debug('mp.endpoints.js was picked up')

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
