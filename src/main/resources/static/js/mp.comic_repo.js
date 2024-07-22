console.log('mp.comic_repo.js was picked up')


const comic_repo = new Map()

export const isComic = (image) => {
    if (image === null) return false
    let {links} = image
    if (!links) return false
    let pageLink = links.find(link => link.rel.includes('page_'))
    return pageLink !== undefined && pageLink !== null
        && pageLink.href !== undefined && pageLink.href !== null
}

export const getComicPages = (comic) => comic.links.filter(link => link.rel.includes('page_'))

export const loadComic = (comic) => {
    if (comic && comic.id)
        comic_repo.set(comic.id, comic)
}

export const loadComics = (comics) => {
    comics && comics.forEach(comic => loadComic(comic))
}

export const fetchComics = () => {
    return comic_repo
}

export const fetchComic = (id) => {
    return comic_repo.get(id)
}

export const fetchComicForPage = (url) => {
    let result = null
    comic_repo.forEach(comic => {
        let pages = getComicPages(comic)
        pages.forEach(page => {
            if (page.href === url) {
                result = comic
            }
        })
    })
    return result
}