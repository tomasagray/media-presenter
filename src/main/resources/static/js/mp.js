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