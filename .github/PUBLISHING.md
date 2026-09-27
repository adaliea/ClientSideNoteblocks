# Publishing

The **Publish mod** GitHub Actions workflow runs manually. Pushes, tags, and
GitHub Releases do not publish anything. It builds the selected ref with Java 25
and publishes the installable jar to Modrinth, CurseForge, or both.

## One-time setup

Add these repository secrets under
[Settings → Secrets and variables → Actions](https://github.com/adaliea/ClientSideNoteblocks/settings/secrets/actions):

| Secret | Value |
| --- | --- |
| `MODRINTH_TOKEN` | A [Modrinth personal access token](https://modrinth.com/settings/pats) with permission to create and edit versions, from an account allowed to publish this project. |
| `CURSEFORGE_TOKEN` | A [CurseForge author API token](https://authors.curseforge.com/account/api-tokens) from an account allowed to upload files to this project. |

Only the token for the selected platform is needed; a dry run needs neither.
Project IDs are already configured: Modrinth `flmhXQgb`, CurseForge `473175`.
The CurseForge token is the author upload token described in the
[upload API documentation](https://support.curseforge.com/support/solutions/articles/9000197321-curseforge-upload-api).

## Release a version

1. Update `mod_version` in `gradle.properties`. If changing Minecraft versions,
   also update `minecraft_version`, the dependencies, and `fabric.mod.json`.
2. Commit and push the version you want to publish.
3. Open **Actions → Publish mod → Run workflow**, select the branch, choose the
   destination and release channel (`release`, `beta`, or `alpha`), and enter
   the release notes. The version and Minecraft compatibility come from the
   selected commit, not from the workflow inputs.
4. Leave **Build and validate only (no publishing)** checked for a dry run.
   Download the `mod-jar` artifact from that run to test the build.
5. Run again with that box unchecked to publish. Keep the selected branch at
   the tested commit, or dispatch against a tag using the GitHub CLI:
   `gh workflow run publish.yml --ref <tag>`.

The workflow uploads only the installable jar, marks Fabric API as required,
Mod Menu as optional, and Cloth Config as embedded. Release notes are used as
the changelog on both sites. CurseForge may take time to approve an upload.

The platforms run as separate jobs. If one succeeds and the other fails, use
**Re-run failed jobs**, or run the workflow with only the failed platform
selected. Check the destination first if a request timed out after uploading;
rerunning a successful publication can create duplicates or fail because the
version already exists.
