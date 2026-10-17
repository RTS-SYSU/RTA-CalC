const { spawnSync } = require('node:child_process')
const path = require('node:path')
const root = path.resolve(__dirname, '..')
const mode = process.argv[2]
const windows = process.platform === 'win32'
function run(command, args, directory) {
  const result = spawnSync(command, args, {
    cwd: path.join(root, directory), stdio: 'inherit', shell: windows
  })
  if (result.error) { console.error(result.error.message); process.exit(1) }
  if (result.status !== 0) process.exit(result.status || 1)
}
const maven = windows ? 'mvnw.cmd' : 'sh'
function server(args) { run(maven, windows ? args : ['./mvnw', ...args], 'server') }
function client(script) { run(windows ? 'npm.cmd' : 'npm', ['run', script], 'client') }
switch (mode) {
  case 'serve': server(['spring-boot:run']); break
  case 'test': server(['test']); client('test:data-chain'); break
  case 'build': server(['package']); client('build'); break
  case 'verify':
    server(['clean', 'package'])
    run(windows ? 'npm.cmd' : 'npm', ['run', 'lint', '--', '--no-fix'], 'client')
    client('test:data-chain')
    client('build')
    break
  default: console.error('Usage: node scripts/project.cjs serve|test|build|verify'); process.exit(1)
}
