const AWS = require('aws-sdk');
//const endpoint = new AWS.Endpoint('http://localhost:10201');
const sqs = new AWS.SQS({
  version: '2012-11-05',
  region: 'ogikubo',
  credentials: new AWS.Credentials('dummy', 'dummy'),
  httpOptions: {
    timeout: 5000
  }
});
sqs.sendMessage({
  MessageBody: "hoge",
  QueueUrl: "http://localhost:10201"
}, (err, data) => {
  if (err) {
    console.log("Error", err);
  } else {
    console.log("Success", data.MessageId);
  }
});
