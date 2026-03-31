# prismo

Prismatic RNG testing.

For actual real numbers:
Randomly selecting a real and removing, infinitely repeated, leaves uncountably many reals unremoved.

For finite binary prefixes of real numbers, this is not the case.

The overall goal here is to use the patterns, if any, of surviving elements in a simulated real domain,
to determine any signature regularities or weaknesses in random number generators.
The immediate goal is to construct an analytics pipeline that can distinguish between Random and SecureRandom.
The punt goal is to construct a RAND16 clone and tell the difference between Random and that.

## FakeR

A "fake real" domain, representing some domain of byte sequences at reduced resolution,
supporting testing whether a byte sequence is still present in it and removing a byte sequence from it.
These domains may be insignificantly or significantly aliased vs. / lower resolution than the input.

## The prismatic test

We allocate some `byte[]` to act as our sample, and repeatedly call our source, a `Consumer<byte[]>`,
the functional interface of e.g. `SecureRandom::nextBytes`.
The expected number of iterations to exhaust the space, given no aliasing and uniform randomness,
is given by `guessPrismaticIterations`.

Statistics are kept on the number of survivors in the `FakeR` tested against.
Each sample, we update iteration count on our statistics,
receive some new bytes,
remove them from our real domain,
and pass the sample and statistics to analytics.

Some (although not necessarily all) `FakeR` implementations are fully materialized and allow inspecting exact survivors.
Some `FakeR` may not be fully materialized (e.g. hypothetical Bloom-filter based) and will not.
