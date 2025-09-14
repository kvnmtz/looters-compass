const axios = require('axios').default;
const FormData = require('form-data');
const fs = require("node:fs");

function escapeControlCharacters(str) {
  return str.replace(/[\0-\x1F\x7F]/g, (char) => {
    switch (char) {
      case '\n':
        return '\\n';
      case '\r':
        return '\\r';
      case '\t':
        return '\\t';
      case '\b':
        return '\\b';
      case '\f':
        return '\\f';
      case '\v':
        return '\\v';
      case '\0':
        return '\\0';
      default:
        return '\\x' + char.charCodeAt(0).toString(16).padStart(2, '0');
    }
  });
}

module.exports = {
  verifyConditions: async (pluginConfig, context) => {
    const {env} = context;
    if (!env.CURSEFORGE_PAT.length) {
      throw AggregateError('No Curseforge personal access token provided');
    }
  },
  success: async (pluginConfig, context) => {
    const {nextRelease} = context;
    const version = nextRelease.version;
    const changelog = escapeControlCharacters(nextRelease.notes);

    const {env} = context;
    const curseforgeToken = env.CURSEFORGE_PAT;

    const formData = new FormData();
    formData.append('metadata', `{
      "changelog": "${changelog}",
      "changelogType": "markdown",
      "displayName": "Looter's Compass ${version}",
      "gameVersions": [9639, 9638, 7498, 10150, 9990],
      "releaseType": "release",
      "relations": {
          "projects": [
              {
                  slug: "lootr",
                  projectID: 361276,
                  type: "requiredDependency"
              }
          ]
      }
    }`);
    formData.append('file', fs.createReadStream(`./build/libs/looters_compass-${version}.jar`));

    let headers = formData.getHeaders();
    headers['X-Api-Token'] = curseforgeToken;

    await axios.post('https://minecraft.curseforge.com/api/projects/1347264/upload-file', formData, {
      headers: headers,
    });
  }
}