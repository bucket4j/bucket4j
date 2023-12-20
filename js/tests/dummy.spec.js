import {Bucket} from './../src/Bucket.js'

describe('Bucket should be imported', function () {
  // asserting JavaScript options
  it('should pass', function () {
    let bucket = new Bucket();
    expect(bucket).toBeDefined()
  })
})