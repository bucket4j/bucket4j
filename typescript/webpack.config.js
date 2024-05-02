const path = require('path');

module.exports = {
  mode: 'development',

  devtool: 'inline-source-map',

  entry: ['./src/Bucket4j.ts'],

  resolve: {
    extensions: ['.ts', '.js'],
  },

  module: {
    rules: [
      {
        test: /\.ts$/,
        use: 'ts-loader',
        exclude: '/node_modules/',
      },
    ],
  },

  output: {
    filename: '[name].bundle.js',
    path: path.resolve(__dirname, 'dist'),
    clean: true,
    globalObject: 'this',
    library: {
      name: 'bucket4j',
      type: 'umd',
    },
  },
};