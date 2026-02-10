import path from "node:path";
import {fileURLToPath} from "node:url";


const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

export default {
    entry: "./js/index.js",
    output: {
        filename: "mp.min.js",
        path: path.resolve(__dirname, "src/main/resources/static/js/"),
    },
    mode: 'production'
}
