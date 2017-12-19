def getVaultAddr() {
    // in-cluster default vault server address; can be overridden below
    def vault_address = 'https://http.vault.svc.cluster.local:8200'

    def env = System.getenv()
    def environment_configured_vault_addr = env['VAULT_ADDR']
    if(environment_configured_vault_addr?.trim()) {
        vault_address = environment_configured_vault_addr
    }
    return vault_address
}

def getVaultCacert() {
    // in-cluster default vault ca certificate; can be overridden below
    def vault_cacertificate = '/var/run/secrets/kubernetes.io/serviceaccount/ca.crt'
    def env = System.getenv()
    def environment_configured_vault_cacert = env['VAULT_CACERT']
    if(environment_configured_vault_cacert?.trim()) {
        vault_cacertificate = environment_configured_vault_cacert
    }
    return vault_cacertificate
}

def getVaultToken() {
    withCredentials([[$class: 'UsernamePasswordMultiBinding',
                      credentialsId: 'vault',
                      usernameVariable: 'VAULT_USER',
                      passwordVariable: 'VAULT_PASSWORD']]) {
        def vault_addr = getVaultAddr()
        def vault_cacert = getVaultCacert()
        def token_auth_cmd = ['sh', '-c', "PATH=/usr/bin VAULT_ADDR=${vault_addr} VAULT_CACERT=${vault_cacert} /usr/local/bin/vault auth -method=ldap username=$VAULT_USER password=$VAULT_PASSWORD"]
        println("Attempting auth with command: " + token_auth_cmd)
        def proc = token_auth_cmd.execute()
        proc.waitFor()
        result = proc.in.text
        resultlist = result.split("\n")
        if (proc.exitValue() == 0 && resultlist.size() >= 3) {
            auth_token = resultlist[3].split(" ")[1].toString()
        } else {
            error('Auth token retrieval failed. ' + \
                  ' stdout: ' + result + \
                  '. stderr: ' + proc.err.text
                 )
        }
        return auth_token
    }
}
