const path = require('path');

module.exports = {
  mode: 'development',
  entry: {
    bucket4j: './src/bucket4j.js'
  },
  devtool: 'inline-source-map',
  output: {
    filename: '[name].bundle.js',
    path: path.resolve(__dirname, 'dist'),
    clean: true,
    globalObject: 'this',
    library: {
      name: 'bucket4j',
      type: 'umd',
    },
  }
};