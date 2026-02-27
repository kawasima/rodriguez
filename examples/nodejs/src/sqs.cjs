const { SQSClient, SendMessageCommand } = require('@aws-sdk/client-sqs');

const sqs = new SQSClient({
  region: 'ogikubo',
  endpoint: 'http://localhost:10214',
  credentials: {
    accessKeyId: 'dummy',
    secretAccessKey: 'dummy',
  },
  requestHandler: {
    requestTimeout: 5000,
  },
});

const command = new SendMessageCommand({
  MessageBody: 'hoge',
  QueueUrl: 'http://localhost:10214',
});

sqs.send(command)
  .then((data) => {
    console.log('Success', data.MessageId);
  })
  .catch((err) => {
    console.log('Error', err);
  });
