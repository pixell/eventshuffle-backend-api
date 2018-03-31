# Eventshuffle Backend API

[Play Framework](https://www.playframework.com) backend application for scheduling events. Data is stored locally in [H2 Database](https://h2database.com).


## Getting started

First you need to have suitable _Java_ and _sbt_ installed. [Play's guide](https://www.playframework.com/documentation/2.6.x/Installing#Prerequisites) is helpful for installing both.

Then you can start the application by executing

```
sbt run
```

When you see a row starting with `(Server started...)` API is available in `http://localhost:9000`. To exit, press `Ctrl+c``.

**Note**: it will take a little more time the first time as it downloads few dependencies. Also the first request served by the application will compile all the sources and therefore it takes more time.

**Note**: H2 Database files are created under `target` directory, which means it will get removed if `sbt clean` is run.


## Running tests

The project contains a test suite to make sure API works as expected. Tests are run by executing

```
sbt test
```

Tests use a separate in-memory database, which is created before tests are run and removed after process ends.
