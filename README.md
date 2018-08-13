### lib-log

Contains utilities for logging.


#### Rollbar
To use the Rollbar logger in your project:

  - Add a `rollbar.token = ${?ROLLBAR_TOKEN}` to your configuration file (e.g. `base.conf`).
    - This is the **project access token** and can be generated at `https://rollbar.com/flow.io/{project name}/settings/access_tokens/`. The scope must be `post_server_item` or higher (e.g. `write`).
  - Enable the module in your configuration file:
    - `play.modules.enabled += "io.flow.log.RollbarModule"`
  - Inject `logger: RollbarLogger` and use it instead of the default Play logger
  - Add [logback.xml](https://github.com/flowcommerce/content/blob/master/api/conf/logback.xml) and [logback-test.xml](https://github.com/flowcommerce/content/blob/master/api/conf/logback-test.xml) to `api/conf`