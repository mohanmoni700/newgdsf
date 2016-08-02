to run the project on qa configuration run the following command
>activator "run -Dhttp.port=9080 -Dconfig.file={path to project}\GDSService\conf\application.dev.conf"

OR

> activator -jvm-debug "run -Dhttp.port=9080 -Dconfig.resource=application.dev.conf"
