/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */

import React, {useState} from 'react';
import type {PropsWithChildren} from 'react';
import {
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  useColorScheme,
  View,
  Button,
  // NativeModules,
} from 'react-native';

import {
  Colors,
  DebugInstructions,
  Header,
  LearnMoreLinks,
  ReloadInstructions,
} from 'react-native/Libraries/NewAppScreen';

import RecordScreen, {RecordingResult} from 'react-native-record-screen';
import RNFS from 'react-native-fs';

type SectionProps = PropsWithChildren<{
  title: string;
}>;

function Section({children, title}: SectionProps): React.JSX.Element {
  const isDarkMode = useColorScheme() === 'dark';
  return (
    <View style={styles.sectionContainer}>
      <Text
        style={[
          styles.sectionTitle,
          {
            color: isDarkMode ? Colors.white : Colors.black,
          },
        ]}>
        {title}
      </Text>
      <Text
        style={[
          styles.sectionDescription,
          {
            color: isDarkMode ? Colors.light : Colors.dark,
          },
        ]}>
        {children}
      </Text>
    </View>
  );
}

// const ScreenRecorder = NativeModules;
// const { ScreenRecorder } = NativeModules;

function App(): React.JSX.Element {
  const isDarkMode = useColorScheme() === 'dark';
  const [recording, setRecording] = useState(false);

  const backgroundStyle = {
    backgroundColor: isDarkMode ? Colors.darker : Colors.lighter,
  };

  const startRecording = async () => {
    console.log('start recording');
    setRecording(true);
    const res = await RecordScreen.startRecording().catch(error =>
      console.error(error),
    );
    if (res === RecordingResult.PermissionError) {
      // user denies access
      console.log('access denied');
    }
  };

  const stopRecording = async () => {
    console.log('stop recording');
    setRecording(false);
    const res = await RecordScreen.stopRecording().catch(error =>
      console.warn(error),
    );
    if (res) {
      const url = (res.result as any).outputURL;
      console.log("url==> ", url)
      
      // Define the path where you want to move the file
      const filename = url.substring(url.lastIndexOf('/') + 1);
    
      // Define the destination path
      const newPath = RNFS.DownloadDirectoryPath + '/' + filename;
      
      try {
        // Move the file to the download folder
        await RNFS.moveFile(url, newPath);
        console.log('File moved successfully!');
        console.log("new path ==> ", newPath);
      } catch (error) {
        console.log('Error moving file: ', error);
      }
    }
  };

  return (
    <SafeAreaView style={backgroundStyle}>
      <StatusBar
        barStyle={isDarkMode ? 'light-content' : 'dark-content'}
        backgroundColor={backgroundStyle.backgroundColor}
      />
      <ScrollView
        contentInsetAdjustmentBehavior="automatic"
        style={backgroundStyle}>
        {/* <Header /> */}
        {/* <View
          style={{
            backgroundColor: isDarkMode ? Colors.black : Colors.white,
          }}>
          <Section title="Step One">
            Edit <Text style={styles.highlight}>App.tsx</Text> to change this
            screen and then come back to see your edits.
          </Section>
          <Section title="See Your Changes">
            <ReloadInstructions />
          </Section>
          <Section title="Debug">
            <DebugInstructions />
          </Section>
          <Section title="Learn More">
            Read the docs to discover what to do next:
          </Section>
          <LearnMoreLinks />
        </View> */}

        <View>
          <Button
            title="Start Recording"
            onPress={startRecording}
            disabled={recording}
          />
          <Button
            title="Stop Recording"
            onPress={stopRecording}
            disabled={!recording}
          />
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  sectionContainer: {
    marginTop: 32,
    paddingHorizontal: 24,
  },
  sectionTitle: {
    fontSize: 24,
    fontWeight: '600',
  },
  sectionDescription: {
    marginTop: 8,
    fontSize: 18,
    fontWeight: '400',
  },
  highlight: {
    fontWeight: '700',
  },
});

export default App;
