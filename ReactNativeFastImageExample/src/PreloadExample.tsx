import React, { useState } from "react";
import { StyleSheet, View, PermissionsAndroid } from "react-native";
import SectionFlex from "./SectionFlex";
import FastImage from "@valery-lavrik/react-native-fast-image";
import Section from "./Section";
import FeatureText from "./FeatureText";
import Button from "./Button";
// @ts-ignore
import { createImageProgress } from "react-native-image-progress";
import { useCacheBust } from "./useCacheBust";

const IMAGE_URL =
    "https://cdn-images-1.medium.com/max/1600/1*-CY5bU4OqiJRox7G00sftw.gif";

// @ts-ignore
const Image = createImageProgress(FastImage);

export const PreloadExample = () => {
    const [show, setShow] = useState(false);
    const { url, bust } = useCacheBust(IMAGE_URL);

    const preload = async () => {
        try {
            await checkPermissions();
        } catch (error) {
            console.log("error", error);
            return;
        }

        FastImage.preloadDimension({
            uri: "https://via.placeholder.com/1000x1100.png",
            // uri: 'file:///storage/emulated/0/Download/GalleryComics_Cache_2/Image/img.com-x.life/comix/10836/207538/111.jpg',
            // headers: { Authorization: 'someAuthToken' },
            saveToFile: "/storage/emulated/0/Download/1112.jpg"
        })
            .then((obj) => {
                console.log("width, height", obj);
            })
            .catch((error) => {
                console.log("error *******************************", error);
            });
    };

    return (
        <View>
            <Section>
                <FeatureText text="• Preloading." />
                <FeatureText text="• Progress indication using react-native-image-progress." />
            </Section>
            <SectionFlex style={styles.section}>
                {show ? (
                    <Image style={styles.image} source={{ uri: url }} />
                ) : (
                    <View style={styles.image} />
                )}
                {/* <FastImage
                    style={styles.image}
                    source={{
                        uri: "file:///storage/emulated/0/Download/1112.jpg"
                    }}
                /> */}
                <View style={styles.buttons}>
                    <View style={styles.buttonView}>
                        <Button text="Bust" onPress={bust} />
                    </View>
                    <View style={styles.buttonView}>
                        <Button text="Preload" onPress={preload} />
                    </View>
                    <View style={styles.buttonView}>
                        <Button
                            text={show ? "Hide" : "Show"}
                            onPress={() => setShow((v) => !v)}
                        />
                    </View>
                </View>
            </SectionFlex>
        </View>
    );
};

const styles = StyleSheet.create({
    buttonView: { flex: 1 },
    section: {
        flexDirection: "column",
        alignItems: "center"
    },
    buttons: {
        flexDirection: "row",
        marginHorizontal: 20,
        marginBottom: 10
    },
    image: {
        backgroundColor: "#ddd",
        margin: 20,
        marginBottom: 10,
        height: 100,
        width: 100
    }
});

export const checkPermissions = () => {
    return new Promise(async function (resolve, reject) {
        try {
            const granted = await PermissionsAndroid.request(
                PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE
            );
            if (granted === PermissionsAndroid.RESULTS.GRANTED) {
                resolve(true);
            } else {
                reject("result no granted: " + granted);
            }
        } catch (err) {
            reject(err);
        }
    });
};
