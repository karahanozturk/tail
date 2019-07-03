This app tails a file and publishes it to AWS S3.

In this application, there are many assumptions made, due to lack of requirements, to keep the test minimal and simple which can be changed later. eg: the app doesn't support multi publishers, it will use timestamp as a key in s3 bucket, IO monad is not needed for now, termination can be done by interruption, error handling is not implemented etc. 

The file path and S3 access is configured in configuration file (src/main/resources/reference.conf)

Please specify the configuration before running the application

### Install sbt
```
./src/main/resources/install/installSbtMac.sh
```

### Compile
```
sbt compile
```

### Run tests
The tests consists of unit test and integration test. To be able to run the integration test, which uses AWS S3, please configure "test.conf" (src/test/resources/test.conf)
```
sbt test
```

### Run application
```
 sbt "runMain AppRunner"
```


### Uninstall sbt
```
./src/main/resources/install/uninstallSbtMac.sh
```
