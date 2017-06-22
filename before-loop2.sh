#!/bin/bash

# h1 ping h3, path is h2 -> S2 -> S3 -> h3

ovs-ofctl -O OpenFlow13 add-flow s3 in_port=*,ip,nw_src=10.0.0.0/24,priority:65535,action=output:3
ovs-ofctl -O OpenFlow13 add-flow s4 in_port=2,ip,nw_src=10.0.0.0/24,priority:65535,action=output:3
ovs-ofctl -O OpenFlow13 add-flow s1 in_port=3,ip,nw_src=10.0.0.0/24,priority:65535,action=output:2
