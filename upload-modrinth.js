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
    if (!env.MODRINTH_PAT.length) {
      throw AggregateError('No Modrinth personal access token provided');
    }
  },
  success: async (pluginConfig, context) => {
    const {nextRelease} = context;
    const version = nextRelease.version;
    const changelog = escapeControlCharacters(nextRelease.notes);

    const {env} = context;
    const modrinthToken = env.MODRINTH_PAT;

    const formData = new FormData();
    formData.append('data', `{
      "name": "Looter's Compass ${version}",
      "version_number": "${version}",
      "changelog": "${changelog}",
      "dependencies": [
        {
          "version_id": null,
          "project_id": "EltpO5cN",
          "file_name": null,
          "dependency_type": "required"
        }
      ],
      "game_versions": [
        "1.20.1"
      ],
      "loaders": [
        "forge",
        "neoforge"
      ],
      "version_type": "release",
      "featured": true,
      "status": "listed",
      "project_id": "u7HUuWes",
      "file_parts": [
        "file"
      ]
    }`);
    formData.append('file', fs.createReadStream(`./build/libs/looters_compass-${version}.jar`));

    let headers = formData.getHeaders();
    headers.authorization = modrinthToken;

    await axios.post('https://api.modrinth.com/v2/version', formData, {
      headers: headers,
    });
  }
}