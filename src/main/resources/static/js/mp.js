console.log('mp.js was picked up')

export const showVideoPlayer = (url) => () => {
  console.log('url', url.substring(28))
  $('#Video-player-container').css('display', 'block')
  let player = $('#Video-player')
  player.attr('src', url)
  player[0].load()
}

export const hideVideoPlayer = () => {
  console.log('hiding video player...')
  let player = $('#Video-player')
  player.attr('src', null)
  player[0].load()
  $('#Video-player-container').css('display', 'none')
}