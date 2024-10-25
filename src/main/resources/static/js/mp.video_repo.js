import {fetchFromRepoAt} from "./mp.js";


console.log('mp.video_repo.js was picked up')

const video_repository = new Map()

export const loadVideo = (video) => {
    if (video && video.id)
        video_repository.set(video.id, video)
}

export const fetchVideoById = (id) => {
    for (let i = 0; i < video_repository.size; i++) {
        let video = fetchFromRepoAt(video_repository, i)
        if (video.id === id) {
            return {
                pos: i,
                data: video,
            }
        }
    }
}