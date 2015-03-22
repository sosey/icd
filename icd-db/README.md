ICD Database
============

This project provides the ICD database interface and command line application, based on MongoDB.
It is assumed that the MongoDB server is running on the given (or default) host and port.

icd-db Command
--------------

The icd-db command is generated in target/universal/stage/bin (install.sh copies it to the install/bin directory).

Example files that can be ingested into the database for testing can be found
in the [examples](../examples) directory.

```
icd-db 0.1-SNAPSHOT
Usage: icd-db [options]

  -d <name> | --db <name>
        The name of the database to use (default: icds)
  -h <hostname> | --host <hostname>
        The host name where the database is running (default: localhost)
  -p <number> | --port <number>
        The port number to use for the database (default: 27017)
  -i <dir> | --ingest <dir>
        Directory containing ICD files to ingest into the database
  -l [hcds|assemblies|all] | --list [hcds|assemblies|all]
        Prints a list of hcds, assemblies, or all components
  -c <name> | --component <name>
        Specifies the component to be used by any following options
  -o <outputFile> | --out <outputFile>
        Saves the component's ICD to the given file in a format based on the file's suffix (md, html, pdf)
  --drop [db|component]
        Drops the specified component or database (use with caution!)
```

Example:
--------

```
> icd-db --ingest examples/NFIRAOS/
> icd-db --list all
  NFIRAOS
  envCtrl
  lgsWfs
  nacqNhrwfs
  ndme
> icd-db --list assemblies
  envCtrl
  lgsWfs
  nacqNhrwfs
  ndme
> icd-db -c NFIRAOS -o NFIRAOS.pdf

```


Implementation
--------------

Each ICD JSON file is stored in its own MongoDB collection.
Here is a listing of the collections present after running this ingest command:


```
> icd-db --ingest examples/NFIRAOS/
> mongo icds
MongoDB shell version: 3.0.0
connecting to: icds
> show collections
NFIRAOS.command
NFIRAOS.component
NFIRAOS.envCtrl.command
NFIRAOS.envCtrl.component
NFIRAOS.envCtrl.icd
NFIRAOS.envCtrl.publish
NFIRAOS.icd
NFIRAOS.lgsWfs.component
NFIRAOS.lgsWfs.icd
NFIRAOS.lgsWfs.publish
NFIRAOS.nacqNhrwfs.command
NFIRAOS.nacqNhrwfs.component
NFIRAOS.nacqNhrwfs.icd
NFIRAOS.nacqNhrwfs.publish
NFIRAOS.ndme.command
NFIRAOS.ndme.component
NFIRAOS.ndme.icd
NFIRAOS.ndme.publish
NFIRAOS.publish
system.indexes

```

The code then looks for collections with names ending in .icd, .component, .publish, .subscribe, or .command.
Queries can be run on all component, icd or command collections, etc.
When detailed information is needed, the JSON in a collection is parsed into the same model classes used to
create the PDF document. Creating documents from the database works in the same way as creating them from files.
The JSON is parsed into model classes and then the document is generated from the model.