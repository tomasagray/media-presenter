import * as videoRepo from "./data/mp.video_repo"
import * as imageRepo from "./data/mp.image_repo";
import * as comicRepo from "./data/mp.comic_repo";
import * as video from "./viewer/mp.video"
import * as image from "./viewer/mp.image";
import * as editModal from "./mp.edit-modal";
import * as search from "./mp.search";
import * as util from "./mp.util";
import * as endpoints from "./mp.endpoints";
import * as state from "./data/mp.state";


window.mp = {
    videoRepo,
    imageRepo,
    comicRepo,
    video,
    image,
    editModal,
    search,
    util,
    endpoints,
    state,
}
