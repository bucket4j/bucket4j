/* eslint-disable */
const fs = require('fs')
const Path = require('path')
/* eslint-enable */

const deleteFolderRecursive = (path) => {
  if (fs.existsSync(path)) {
    fs.readdirSync(path).forEach((file) => {
      const curPath = Path.join(path, file)
      if (fs.lstatSync(curPath).isDirectory()) {
        deleteFolderRecursive(curPath)
      } else {
        fs.unlinkSync(curPath)
      }
    })
    fs.rmdirSync(path)
  }
}

const folder = process.argv.slice(2)[0]

if (folder) {
  deleteFolderRecursive(Path.join(__dirname, '../dist', folder))
} else {
  deleteFolderRecursive(Path.join(__dirname, '../dist/cjs'))
  deleteFolderRecursive(Path.join(__dirname, '../dist/esm'))
  deleteFolderRecursive(Path.join(__dirname, '../dist/umd'))
  deleteFolderRecursive(Path.join(__dirname, '../dist/types'))
}
