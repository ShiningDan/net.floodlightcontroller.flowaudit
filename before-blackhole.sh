#!/bin/bash

# has h1 -> S1 -> S2 -> S3 -> h3 already

ovs-ofctl -O OpenFlow13 add-flow s1 in_port=1,ip,nw_src=10.0.0.0/24,priority:65535,action=output:2
ovs-ofctl -O OpenFlow13 add-flow s2 in_port=2,ip,nw_src=10.0.0.0/24,priority:65535,action=output:3
ovs-ofctl -O OpenFlow13 add-flow s3 in_port=2,ip,nw_src=10.0.0.0/24,priority:65535,action=drop
