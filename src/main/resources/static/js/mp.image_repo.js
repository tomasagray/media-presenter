console.log('mp.image_repo.js was picked up')


const image_repository = new Map()

export const loadImages = (images) => {
    images && images.forEach(image => loadImage(image))
}

export const loadImage = (image) => {
    // console.log('loading image', image)
    if (image && image.id)
        image_repository.set(image.id, image)
}

export const fetchImages = () => {
    return image_repository
}

export const fetchImageById = (id) => {
    for (let i = 0; i < image_repository.size; i++) {
        let img = fetchImageAtPosition(i)
        if (img.id === id) {
            return {
                pos: i,
                data: img
            }
        }
    }
}

const getImageLink = (img, rel) => {
    return img.links.filter(link => link.rel === rel)[0]
}

export const fetchImageAt = (url) => {
    let image = null
    for (let i = 0; i < image_repository.size; i++) {
        let img = fetchImageAtPosition(i)
        const dataLink = getImageLink(img, 'data')
        if (dataLink.href === url) {
            return {
                pos: i,
                data: img
            }
        }
    }
    return image
}

export const fetchImageAtPosition = (pos) => {
    let i = 0, requested = null
    let values = image_repository.values()
    while (i <= pos) {
        requested = values.next().value
        i++
    }
    return requested
}

export const fetchPictureCount = () => {
    return image_repository.size
}