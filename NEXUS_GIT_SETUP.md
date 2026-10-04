# Nexus GitHub Repository Setup

This guide creates a **new Git repository** for the cleaned project and pushes it to the Nexus GitHub organization.

Recommended repository name:

```text
blockchain-network-simulation
```

Recommended description:

> Simulation-based study of blockchain network decentralization, block propagation and fault tolerance using Java, graph algorithms and interactive web visualizations.

---

## Before you start

1. Extract the cleaned project ZIP.
2. Rename the extracted folder to:

```text
blockchain-network-simulation
```

3. Open that folder in VS Code.
4. Open **PowerShell** in the project root.

You should see files such as:

```text
pom.xml
README.md
NEXUS_GIT_SETUP.md
src/
output/
website/
```

Check with:

```powershell
Get-ChildItem
```

---

## 1. Check whether old Git metadata exists

```powershell
Test-Path .git
```

If it returns `True`, remove the old Git metadata so this becomes a completely new repository:

```powershell
Remove-Item -Recurse -Force .git
```

Confirm:

```powershell
Test-Path .git
```

Expected:

```text
False
```

> This deletes only Git history/configuration inside `.git`. It does not delete the project source files.

---

## 2. Optional pre-push validation

### Java

If Maven is installed:

```powershell
java -version
mvn -version
mvn clean test
```

### Website

```powershell
cd website
npm ci
npm run build
cd ..
```

The website build output should be created under `website/dist/`. It is ignored by Git.

---

## 3. Quick secret check

Run from the repository root:

```powershell
Get-ChildItem -Recurse -Force -File |
Select-String -Pattern "API_KEY|SECRET_KEY|PASSWORD|PRIVATE_KEY|VERCEL_TOKEN|GITHUB_TOKEN" |
Select-Object Path, LineNumber, Line
```

Also check for environment files:

```powershell
Get-ChildItem -Recurse -Force -Filter ".env*"
```

Do not commit real credentials or tokens.

---

## 4. Initialize the new Git repository

```powershell
git init -b main
```

Check status:

```powershell
git status
```

The root `.gitignore` should prevent build artifacts such as `target/`, `node_modules/`, and `website/dist/` from being staged.

---

## 5. Stage and inspect the project

```powershell
git add .
git status
```

You should see source files, Java outputs, website source, README files, and `.gitignore`.

You should **not** see:

```text
node_modules/
website/node_modules/
website/dist/
target/
.env
```

If any of those appear, stop and fix `.gitignore` before committing.

---

## 6. Create the first commit

```powershell
git commit -m "Initial release: blockchain network simulation PBL"
```

Verify:

```powershell
git log --oneline -5
```

---

## 7. Create the empty repository in the Nexus organization

On GitHub, open the **Nexus organization** and choose **New repository**.

Use:

```text
Repository name:
blockchain-network-simulation
```

Description:

```text
Simulation-based study of blockchain network decentralization, block propagation and fault tolerance using Java, graph algorithms and interactive web visualizations.
```

Choose **Public** or **Private** according to Nexus policy.

Because the project already has a local README and `.gitignore`, create the GitHub repository **empty**:

- do not add a GitHub-generated README;
- do not add another `.gitignore`;
- do not select a license unless Nexus has already chosen one for the project.

---

## 8. Connect the local repository to Nexus

Replace `<NEXUS_ORG>` with the exact GitHub organization slug.

```powershell
git remote add origin https://github.com/<NEXUS_ORG>/blockchain-network-simulation.git
```

Verify:

```powershell
git remote -v
```

Expected pattern:

```text
origin  https://github.com/<NEXUS_ORG>/blockchain-network-simulation.git (fetch)
origin  https://github.com/<NEXUS_ORG>/blockchain-network-simulation.git (push)
```

---

## 9. Push to GitHub

```powershell
git push -u origin main
```

Refresh the Nexus repository page after the push succeeds.

---

## Complete copy-paste block

Use this only after the empty GitHub repository has been created.

```powershell
# Open PowerShell in the extracted project root.

# Remove old Git history only if it exists.
if (Test-Path .git) {
    Remove-Item -Recurse -Force .git
}

# Start a new repository.
git init -b main

# Inspect, stage, and inspect again.
git status
git add .
git status

# Create the first commit.
git commit -m "Initial release: blockchain network simulation PBL"

# Connect to Nexus. REPLACE <NEXUS_ORG> FIRST.
git remote add origin https://github.com/<NEXUS_ORG>/blockchain-network-simulation.git

# Verify and push.
git remote -v
git push -u origin main
```

---

## Optional GitHub CLI method

If GitHub CLI is installed and authenticated:

```powershell
gh auth status
```

Then, from the project root:

```powershell
gh repo create <NEXUS_ORG>/blockchain-network-simulation `
  --public `
  --description "Simulation-based study of blockchain network decentralization, block propagation and fault tolerance using Java, graph algorithms and interactive web visualizations." `
  --source . `
  --remote origin `
  --push
```

Use `--private` instead of `--public` if required by Nexus policy.

---

## Repository topics

After the first push, add these topics from GitHub's **About** settings:

```text
blockchain
computer-networks
java
network-simulation
gossip-protocol
graph-algorithms
fault-tolerance
react
vite
pbl
```

---

## Vercel deployment after the push

In Vercel choose **New Project**, import:

```text
<NEXUS_ORG>/blockchain-network-simulation
```

Use:

```text
Root Directory:    website
Framework Preset:  Vite
Build Command:     npm run build
Output Directory:  dist
```

After deployment, add the generated Vercel URL to both `README.md` and `website/README.md`, then commit it:

```powershell
git add README.md website/README.md
git commit -m "docs: add live Vercel deployment"
git push
```

---

## Recommended later commit for Java metric corrections

If the Java experiment-metric calculations are corrected later, keep that as a separate commit:

```powershell
git add .
git commit -m "fix: correct experiment metrics and regenerate reference results"
git push
```

This keeps the repository history clean and easy to review.
