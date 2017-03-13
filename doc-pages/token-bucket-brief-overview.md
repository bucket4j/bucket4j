### Algorithm in high level

The token bucket algorithm can be conceptually understood as follows:

* A token is added to the bucket every `1/r` seconds.
* The bucket can hold at the most `b` tokens. If a token arrives when the bucket is full, it is discarded.
* When trying to consume `n` tokens, `n` tokens are removed from bucket.
* If fewer than `n` tokens are available, then consumption is disallowed.

See for more details:

* [Wikipedia - Token Bucket](http://en.wikipedia.org/wiki/Token_bucket)
* [Wikipedia - Leaky Bucket](http://en.wikipedia.org/wiki/Leaky_bucket)
* [Wikipedia - Generic cell rate algorithm](http://en.wikipedia.org/wiki/Generic_cell_rate_algorithm)