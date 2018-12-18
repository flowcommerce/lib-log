### lib-log

Contains utilities for logging.


#### Rollbar

To create a project:
- go [here](https://rollbar.com/settings/accounts/flow.io/projects/?project_selector=1#create)
- enter the service name and select Eastern as the timezone
- after clicking "create", select "Java" as the primary SDK

Then, to add the Rollbar logger to a project:

  - Add `"io.flow" %% "lib-log" % "0.0.43",` to build.sbt
  - Set the Rollbar token: `dev env set --keyval ROLLBAR_TOKEN=ACCESS_TOKEN_GOES_HERE --env production --app APP_NAME_GOES_HERE`
    - This is the **project access token** and can be generated at `https://rollbar.com/flow.io/{project name}/settings/access_tokens/`. The scope must be `post_server_item` or higher (e.g. `write`).
  - Add [logback.xml](https://github.com/flowcommerce/misc/blob/master/log/templates/logback.xml) and [logback-test.xml](https://github.com/flowcommerce/misc/blob/master/log/templates/logback-test.xml) to `api/conf`

Lastly, to use Rollbar:
  - Inject `logger: RollbarLogger` and use it instead of the default Play logger
  - Look at an example [here](https://github.com/flowcommerce/label/blob/76f684401719be46143b3b116523d37f48620f0a/api/app/db/ShippingLabelsDao.scala#L464-L473) on how to use it
  - Look at the [source code](https://github.com/flowcommerce/lib-log/blob/master/src/main/scala/io/flow/log/Rollbar.scala#L159) to see how it works under the hood
  - Note that currently only `warn` and `error` log levels are sent to the Rollbar API. Other log statements continue to get logged in the logs (and Sumo)
  - We recommend replacing all calls to `Play.api.logger` w/ `RollbarLogger` - makes it easy to see there is a single logger plus will give us consistency in our log statements.
