console.log('mp.state.js was picked up')


let _state = {}

export const setState = (state) => {
    _state = {
        ..._state,
        ...state
    }
}

export const getState = () => {
    return {
        ..._state
    }
}