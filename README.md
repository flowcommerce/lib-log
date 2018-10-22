### lib-log

Contains utilities for logging.


#### Rollbar

To create a project:
- go [here](https://rollbar.com/settings/accounts/flow.io/projects/?project_selector=1#create)
- enter the service name and select Eastern as the timezone
- after clicking "create", select "Java" as the primary SDK
- the access token should be under the `rollbar-java` header; set it in production using the following command:
  - `dev env set --keyval ROLLBAR_TOKEN=ACCESS_TOKEN_GOES_HERE --env production --app APP_NAME_GOES_HERE`

Then, to add the Rollbar logger to a project:

  - Add `"io.flow" %% "lib-log" % "0.0.29",` to build.sbt
  - Add a `rollbar.token = ${?ROLLBAR_TOKEN}` to your configuration file (e.g. `base.conf`).
    - This is the **project access token** and can be generated at `https://rollbar.com/flow.io/{project name}/settings/access_tokens/`. The scope must be `post_server_item` or higher (e.g. `write`).
  - Enable the module in your configuration file:
    - `play.modules.enabled += "io.flow.log.RollbarModule"`
  - Add [logback.xml](https://github.com/flowcommerce/label/blob/master/api/conf/logback.xml) and [logback-test.xml](https://github.com/flowcommerce/label/blob/master/api/conf/logback-test.xml) to `api/conf`

Lastly, to use Rollbar:
  - Inject `logger: RollbarLogger` and use it instead of the default Play logger
  - Look at an example [here](https://github.com/flowcommerce/label/blob/76f684401719be46143b3b116523d37f48620f0a/api/app/db/ShippingLabelsDao.scala#L464-L473) on how to use it
  - Look at the [source code](https://github.com/flowcommerce/lib-log/blob/master/src/main/scala/io/flow/log/Rollbar.scala#L159) to see how it works under the hood
  - Note that currently only `warn` and `error` log levels are sent to the Rollbar API. Other log statements continue to get logged in the logs (and Sumo)
  - We recommend replacing all calls to `Play.api.logger` w/ `RollbarLogger` - makes it easy to see there is a single logger plus will give us consistency in our log statements.
