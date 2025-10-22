/**
 * @type {import('semantic-release').GlobalConfig}
 */
module.exports = {
  branches: ['main'],
  plugins: [
    [
      '@semantic-release/commit-analyzer',
      {
        preset: 'angular',
        releaseRules: [
          { type: 'tweak', release: 'patch' }
        ]
      }
    ],
    // --------------------
    [
      '@semantic-release/release-notes-generator',
      {
        preset: 'conventionalcommits',
        presetConfig: {
          types: [
            { type: 'feat', section: '✨ Features' },
            { type: 'fix', section: '🐛 Bug Fixes' },
            { type: 'perf', section: '⚡ Performance Improvements' },
            { type: 'revert', section: '↩️ Reverts' },
            { type: 'tweak', section: '⚙️ Tweaks', hidden: false },
            { type: 'docs', section: '📝 Documentation', hidden: true },
            { type: 'style', section: '💈 Styles', hidden: true },
            { type: 'chore', section: '🧹 Miscellaneous Chores', hidden: true },
            { type: 'refactor', section: '🪄 Code Refactoring', hidden: true },
            { type: 'test', section: '✅ Tests', hidden: true },
            { type: 'ci', section: '🔁 Continuous Integration', hidden: true },
          ],
        },
      },
    ],
    // --------------------
    [
      '@semantic-release/changelog',
      {
        changelogFile: 'CHANGELOG.md',
      },
    ],
    // --------------------
    './update-version.js',
    // --------------------
    [
      '@semantic-release/git',
      {
        assets: [
          'gradle.properties',
          'CHANGELOG.md',
        ],
        message: 'chore(release): ${nextRelease.version} [skip ci]',
      },
    ],
    // --------------------
    [
      '@semantic-release/exec',
      {
        prepareCmd: './gradlew build --build-cache',
      },
    ],
    // --------------------
    [
      '@semantic-release/github',
      {
        assets: [
          'build/libs/!(*-@(dev|sources|javadoc)).jar',
        ],
      },
    ],
    // --------------------
    'semantic-release-export-data',
  ],
};
